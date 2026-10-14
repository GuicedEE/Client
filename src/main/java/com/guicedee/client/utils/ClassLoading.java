package com.guicedee.client.utils;

import io.github.classgraph.ClassInfo;

import java.util.List;
import java.util.Objects;

/**
 * Loads discovered classes without initializing them. ClassGraph 6 retains metadata only;
 * the application owns the classloaders used to turn that metadata into runtime classes.
 */
public final class ClassLoading {
    private ClassLoading() {
    }

    /** Resolves boot-layer modules, then the calling thread's application classloader. */
    public static Class<?> loadClass(ClassInfo classInfo) {
        Objects.requireNonNull(classInfo, "classInfo");
        var moduleInfo = classInfo.getModuleInfo();
        if (moduleInfo != null) {
            var module = ModuleLayer.boot().findModule(moduleInfo.getName());
            if (module.isPresent()) {
                try {
                    Class<?> loaded = Class.forName(module.get(), classInfo.getName());
                    if (loaded != null) {
                        return loaded;
                    }
                } catch (LinkageError e) {
                    throw new IllegalArgumentException("Unable to load discovered class " + classInfo.getName(), e);
                }
            }
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ClassLoading.class.getClassLoader();
        }
        return loadClass(classInfo, loader);
    }

    /** Use the same application-owned loader that was supplied to the scanner. */
    public static Class<?> loadClass(ClassInfo classInfo, ClassLoader loader) {
        Objects.requireNonNull(classInfo, "classInfo");
        try {
            return Class.forName(classInfo.getName(), false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalArgumentException("Unable to load discovered class " + classInfo.getName(), e);
        }
    }

    public static List<Class<?>> loadClasses(Iterable<ClassInfo> classes) {
        var loaded = new java.util.ArrayList<Class<?>>();
        for (ClassInfo classInfo : classes) {
            loaded.add(loadClass(classInfo));
        }
        return List.copyOf(loaded);
    }

    public static <T> List<Class<? extends T>> loadClasses(Iterable<ClassInfo> classes, Class<T> type) {
        var loaded = new java.util.ArrayList<Class<? extends T>>();
        for (ClassInfo classInfo : classes) {
            loaded.add(loadClass(classInfo).asSubclass(type));
        }
        return List.copyOf(loaded);
    }
}
