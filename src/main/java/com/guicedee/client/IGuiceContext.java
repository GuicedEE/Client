package com.guicedee.client;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.guicedee.client.services.IGuiceConfig;
import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import com.guicedee.client.services.IGuiceProvider;
import com.guicedee.client.annotations.INotEnhanceable;
import com.guicedee.client.annotations.INotInjectable;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.vertx.core.Future;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public interface IGuiceContext
{

		Map<String, IGuiceContext> contexts = new HashMap<>();
		Set<String> registerModuleForScanning = new LinkedHashSet<>();
		List<com.google.inject.Module> modules = new ArrayList<>();
		Map<Class, Set> allLoadedServices = new LinkedHashMap<>();
		
		static IGuiceContext getContext()
		{
				System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
				System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
				
				if (contexts.isEmpty())
				{
						ServiceLoader<IGuiceProvider> load = ServiceLoader.load(IGuiceProvider.class);
						for (IGuiceProvider iGuiceProvider : load)
						{
								IGuiceContext iGuiceContext = iGuiceProvider.get();
								contexts.put("default", iGuiceContext);
						//		//LogManager.getLogger(IGuiceContext.class).info("✅ Guice context initialized successfully");
								break;
						}
				}
				var out = contexts.get("default");
				if (out == null)
				{
						//LogManager.getLogger(IGuiceContext.class).error("❌ No Guice Contexts have been registered");
						throw new RuntimeException("No Guice Contexts have been registered. Please add com.guicedee:guice-injection to the dependencies");
				}
				////LogManager.getLogger(IGuiceContext.class).trace("📤 Returning Guice context");
				return out;
		}
		
		static IGuiceContext instance()
		{
				IGuiceContext context = getContext();
				////LogManager.getLogger(IGuiceContext.class).trace("📤 Returning Guice context instance");
				return context;
		}
		
		static Map<Class, Set> getAllLoadedServices()
		{
				////LogManager.getLogger(IGuiceContext.class).trace("📋 Getting all loaded services");
				return allLoadedServices;
		}
		
		Future<Void> getLoadingFinished();
		
		Injector inject();
		
		IGuiceConfig<?> getConfig();
		
		void destroy();
		
		static <T> T get(Key<T> type)
		{
				////LogManager.getLogger(IGuiceContext.class).trace("📋 Getting instance for type: {}", type);
				@SuppressWarnings("unchecked")
				Class<T> clazz = (Class<T>) type
																																	.getTypeLiteral()
																																	.getRawType();
				T instance;
				boolean isEntityType = isEntityType(clazz);
				if (isNotEnhanceable(clazz) || isEntityType)
				{
						////LogManager.getLogger(IGuiceContext.class).trace("📝 Type '{}' is not enhanceable or is an entity type, creating new instance", clazz.getCanonicalName());
						try
						{
								instance = clazz
																				.getDeclaredConstructor()
																				.newInstance();
								if (!isNotInjectable(clazz))
								{
										////LogManager.getLogger(IGuiceContext.class).trace("💉 Injecting members for instance of type: {}", clazz.getCanonicalName());
										getContext()
											.inject()
											.injectMembers(instance)
										;
								}
						}
						catch (Exception e)
						{
								//LogManager.getLogger(IGuiceContext.class).error("❌ Failed to construct entity '{}': {}", clazz.getCanonicalName(), e.getMessage(), e);
								throw new RuntimeException(e);
						}
				}
				else
				{
						////LogManager.getLogger(IGuiceContext.class).trace("🔍 Getting instance from injector for type: {}", clazz.getCanonicalName());
						instance = getContext()
																		.inject()
																		.getInstance(type);
				}
				////LogManager.getLogger(IGuiceContext.class).trace("✅ Successfully retrieved instance for type: {}", clazz.getCanonicalName());
				return instance;
		}
		
		private static boolean isNotEnhanceable(Class<?> clazz)
		{
				return clazz.isAnnotationPresent(INotEnhanceable.class);
		}
		
		private static boolean isNotInjectable(Class<?> clazz)
		{
				return clazz.isAnnotationPresent(INotInjectable.class);
		}
		
		private static boolean isEntityType(Class<?> clazz)
		{
				try
				{
						for (Annotation annotation : clazz.getAnnotations())
						{
								if (annotation
													.annotationType()
													.getCanonicalName()
													.equalsIgnoreCase("jakarta.persistence.Entity"))
								{
										return true;
								}
						}
				}
				catch (NullPointerException npe)
				{
						return false;
				}
				return false;
		}
		
		static <T> T get(Class<T> type, Class<? extends Annotation> annotation)
		{
				if (annotation == null)
				{
						return get(Key.get(type));
				}
				return get(Key.get(type, annotation));
		}
		
		static <T> T get(Class<T> type)
		{
				return get(type, null);
		}
		
		ScanResult getScanResult();
		
		Map<String, Set<Class>> loaderClasses = new ConcurrentHashMap<>();
		
		static <T extends Comparable<T>> Set<T> loaderToSet(ServiceLoader<T> loader)
		{
				////LogManager.getLogger(IGuiceContext.class).trace("📋 Loading service set for: {}", loader);
				@SuppressWarnings("rawtypes")
				Set<Class> loadeds = new HashSet<>();
				
				String type = loader.toString();
				type = type.replace("java.util.ServiceLoader[", "");
				type = type.substring(0, type.length() - 1);
				////LogManager.getLogger(IGuiceContext.class).trace("🔍 Resolved service type: {}", type);
				
				if (!loaderClasses.containsKey(type))
				{
						////LogManager.getLogger(IGuiceContext.class).trace("📝 Service type not in cache, loading implementations");
						IGuiceConfig<?> config = getContext().getConfig();
						if (config.isServiceLoadWithClassPath())
						{
								////LogManager.getLogger(IGuiceContext.class).trace("🔍 Scanning classpath for implementations of: {}", type);
								for (ClassInfo classInfo : instance()
																																				.getScanResult()
																																				.getClassesImplementing(type))
								{
										Class<T> load = (Class<T>) classInfo.loadClass();
										loadeds.add(load);
										//LogManager.getLogger(IGuiceContext.class).debug("✅ Found implementation class: {}", load.getName());
								}
						}
						try
						{
								////LogManager.getLogger(IGuiceContext.class).trace("🔄 Loading service implementations using ServiceLoader");
								for (T newInstance : loader)
								{
										loadeds.add(newInstance.getClass());
										//LogManager.getLogger(IGuiceContext.class).debug("✅ Loaded implementation: {}",
																		/*		newInstance
																					.getClass()
																					.getName());*/
								}
						}
						catch (Throwable T)
						{
								//LogManager.getLogger(IGuiceContext.class).error("❌ Failed to provide instance of '{}' to TreeSet: {}", type, T.getMessage(), T);
						}
						////LogManager.getLogger(IGuiceContext.class).trace("💾 Caching loaded classes for service type: {}", type);
						loaderClasses.put(type, loadeds);
				}
				else
				{
						////LogManager.getLogger(IGuiceContext.class).trace("📋 Using cached implementations for service type: {}", type);
				}
				
				////LogManager.getLogger(IGuiceContext.class).trace("🔗 Creating instances for all implementation classes");
				Set<T> outcomes = new TreeSet<>();
				for (Class<?> aClass : loaderClasses.get(type))
				{
						////LogManager.getLogger(IGuiceContext.class).trace("📝 Creating instance for implementation: {}", aClass.getName());
						outcomes.add((T) IGuiceContext.get(aClass));
				}
				////LogManager.getLogger(IGuiceContext.class).trace("✅ Successfully loaded to set {} implementations for service: {}", outcomes.size(), type);
				return outcomes;
		}
		
		static <T> Set<T> loaderToSetNoInjection(ServiceLoader<T> loader)
		{
				////LogManager.getLogger(IGuiceContext.class).trace("📋 Loading service set without injection for: {}", loader);
				Set<Class<T>> loadeds = new HashSet<>();
				IGuiceConfig<?> config = getContext().getConfig();
				String type = loader.toString();
				type = type.replace("java.util.ServiceLoader[", "");
				type = type.substring(0, type.length() - 1);
				////LogManager.getLogger(IGuiceContext.class).trace("🔍 Resolved service type: {}", type);
				
				if (config.isServiceLoadWithClassPath() && instance().getScanResult() != null)
				{
						////LogManager.getLogger(IGuiceContext.class).trace("🔍 Scanning classpath for implementations of: {}", type);
						for (ClassInfo classInfo : instance()
																																		.getScanResult()
																																		.getClassesImplementing(type))
						{
								Class<T> load = (Class<T>) classInfo.loadClass();
								loadeds.add(load);
								//LogManager.getLogger(IGuiceContext.class).debug("✅ Found implementation class: {}", load.getName());
						}
						////LogManager.getLogger(IGuiceContext.class).trace("📊 Found {} implementation classes from classpath scan", loadeds.size());
				}
				else
				{
						////LogManager.getLogger(IGuiceContext.class).trace("📝 Skipping classpath scan for service implementations");
				}
				
				Set<Class<T>> completed = new LinkedHashSet<>();
				Set<T> output = new LinkedHashSet<>();
				try
				{
						//LogManager.getLogger(IGuiceContext.class).debug("🔄 Loading service implementations using ServiceLoader");
						for (T newInstance : loader)
						{
								output.add(newInstance);
								completed.add((Class<T>) newInstance.getClass());
								////LogManager.getLogger(IGuiceContext.class).trace("✅ Loaded implementation instance: {}",
														/*				newInstance
																			.getClass()
																			.getName());*/
						}
						////LogManager.getLogger(IGuiceContext.class).trace("📊 Loaded {} implementation instances from ServiceLoader", completed.size());
				}
				catch (java.util.ServiceConfigurationError T)
				{
						//LogManager.getLogger(IGuiceContext.class).warn("⚠️ Cannot load services for '{}': {}", type, T.getMessage(), T);
				}
				catch (Throwable T)
				{
						//LogManager.getLogger(IGuiceContext.class).error("❌ Cannot load services for '{}': {}", type, T.getMessage(), T);
				}
				
				//LogManager.getLogger(IGuiceContext.class).info("✅ Successfully loaded to set no injection {} implementations for service: {}", output.size(), type);
				return output;
		}
		
		static <T extends Comparable<T>> Set<Class<T>> loadClassSet(ServiceLoader<T> loader)
		{
				////LogManager.getLogger(IGuiceContext.class).trace("📋 Loading class set for service: {}", loader);
				String type = loader.toString();
				type = type.replace("java.util.ServiceLoader[", "");
				type = type.substring(0, type.length() - 1);
				////LogManager.getLogger(IGuiceContext.class).trace("🔍 Resolved service type: {}", type);
				
				if (!loaderClasses.containsKey(type))
				{
						////LogManager.getLogger(IGuiceContext.class).trace("📝 Service type not in cache, loading implementation classes");
						Set<Class> loadeds = new HashSet<>();
						IGuiceConfig<?> config = getContext().getConfig();
						if (config.isServiceLoadWithClassPath())
						{
								//LogManager.getLogger(IGuiceContext.class).debug("🔍 Scanning classpath for implementations of: {}", type);
								for (ClassInfo classInfo : instance()
																																				.getScanResult()
																																				.getClassesImplementing(type))
								{
										@SuppressWarnings("unchecked")
										Class<T> load = (Class<T>) classInfo.loadClass();
										loadeds.add(load);
										////LogManager.getLogger(IGuiceContext.class).trace("✅ Found implementation class: {}", load.getName());
								}
								////LogManager.getLogger(IGuiceContext.class).trace("📊 Found {} implementation classes from classpath scan", loadeds.size());
						}
						else
						{
								////LogManager.getLogger(IGuiceContext.class).trace("📝 Skipping classpath scan for service implementations");
						}
						
						try
						{
								////LogManager.getLogger(IGuiceContext.class).trace("🔄 Loading service implementation classes using ServiceLoader");
								for (T newInstance : loader)
								{
										//noinspection unchecked
										Class<T> implementationClass = (Class<T>) newInstance.getClass();
										loadeds.add(implementationClass);
										////LogManager.getLogger(IGuiceContext.class).trace("✅ Loaded implementation class: {}", implementationClass.getName());
								}
								////LogManager.getLogger(IGuiceContext.class).trace("📊 Loaded {} implementation classes from ServiceLoader", loadeds.size());
						}
						catch (Throwable T)
						{
								//LogManager.getLogger(IGuiceContext.class).error("❌ Failed to provide instance of '{}' to TreeSet: {}", type, T.getMessage(), T);
								////LogManager.getLogger(IGuiceContext.class).trace("🔍 Failure context - Service type: {}, Error type: {}",
							/*											type,
																		T
																			.getClass()
																			.getName());*/
						}
						
						////LogManager.getLogger(IGuiceContext.class).trace("💾 Caching loaded classes for service type: {}", type);
						loaderClasses.put(type, loadeds);
						//LogManager.getLogger(IGuiceContext.class).info("✅ Successfully loaded class set {} implementation classes for service: {}", loadeds.size(), type);
				}
				else
				{
						////LogManager.getLogger(IGuiceContext.class).trace("📋 Using cached implementation classes for service type: {}", type);
				}
				
				//noinspection unchecked
				Set<Class<T>> result = (Set) loaderClasses.get(type);
				////LogManager.getLogger(IGuiceContext.class).trace("📤 Returning {} implementation classes for service: {}", result.size(), type);
				return result;
		}
		
		<T extends Comparable<T>> Set<T> getLoader(Class<T> loaderType, ServiceLoader<T> serviceLoader);
		
		<T> Set<T> getLoader(Class<T> loaderType, @SuppressWarnings("unused") boolean dontInject, ServiceLoader<T> serviceLoader);
		
		boolean isBuildingInjector();
		
		/**
			* Registers a module for scanning when filtering is enabled
			*
			* @param javaModuleName The name in the module-info.java file
			* @return This instance
			*/
		@SuppressWarnings("unchecked")
		static void registerModule(String javaModuleName)
		{
				//LogManager.getLogger(IGuiceContext.class).info("🚀 Registering Java module for scanning: '{}'", javaModuleName);
				registerModuleForScanning.add(javaModuleName);
				//LogManager.getLogger(IGuiceContext.class).debug("🔍 Setting config to include modules and jars");
				instance()
					.getConfig()
					.setIncludeModuleAndJars(true)
				;
				//LogManager.getLogger(IGuiceContext.class).debug("✅ Java module '{}' registered successfully", javaModuleName);
		}
		
		/**
			* Adds a guice module to the injector for processing
			*
			* @param module The Guice module to register
			*/
		static void registerModule(com.google.inject.Module module)
		{
			/*	//LogManager.getLogger(IGuiceContext.class).info("🚀 Registering Guice module: '{}'",
													module
														.getClass()
														.getName());*/
				modules.add(module);
				////LogManager.getLogger(IGuiceContext.class).trace("✅ Guice module '{}' registered successfully",
													/*	module
															.getClass()
															.getName());*/
		}
		
		Set<IGuicePreDestroy> loadPreDestroyServices();
		
		Set<IGuicePreStartup> loadPreStartupServices();
}
