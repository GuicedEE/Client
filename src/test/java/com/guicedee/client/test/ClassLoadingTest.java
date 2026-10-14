package com.guicedee.client.test;

import com.guicedee.client.utils.ClassLoading;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassLoadingTest {
    private static int initializations;
    @TempDir
    Path classpath;

    public interface Service {
    }

    public static class Base implements Service {
    }

    public static class Implementation extends Base {
        static {
            initializations++;
        }
    }

    private ScanResult scanFixtures() {
        return new ClassGraph()
                .enableClasspathEntries(getClass().getProtectionDomain().getCodeSource().getLocation())
                .enableClassInfo().ignoreClassVisibility()
                .acceptPackages(getClass().getPackageName()).scan();
    }

    @Test
    void loadsTransitiveImplementationsWithoutInitializingThem() {
        try (var scan = scanFixtures()) {
            var classes = scan.getAllClassesImplementing(Service.class);
            assertTrue(classes.getNames().contains(Implementation.class.getName()));
            assertTrue(scan.getAllSubclasses(Base.class).getNames().contains(Implementation.class.getName()));
            assertEquals(Implementation.class, ClassLoading.loadClass(scan.getClassInfo(Implementation.class.getName())));
            assertEquals(0, initializations);
            assertTrue(ClassLoading.loadClasses(classes, Service.class).contains(Implementation.class));
            assertThrows(UnsupportedOperationException.class, classes::clear);
        }
    }

    @Test
    void usesTheExplicitApplicationOwnedLoader() throws Exception {
        // An independent classpath namespace has no module-info and no delegation to the test module.
        for (Class<?> fixture : List.of(Base.class, Service.class)) {
            String resource = fixture.getName().replace('.', '/') + ".class";
            Path target = classpath.resolve(resource);
            Files.createDirectories(target.getParent());
            try (var input = fixture.getResourceAsStream("/" + resource)) {
                Files.copy(input, target);
            }
        }
        try (var scan = new ClassGraph().enableClasspathEntries(classpath).enableClassInfo().scan();
             var loader = new URLClassLoader(new java.net.URL[] { classpath.toUri().toURL() }, null)) {
            var metadata = scan.getClassInfo(Base.class.getName());
            assertSame(loader, ClassLoading.loadClass(metadata, loader).getClassLoader());
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(loader);
                assertSame(loader, ClassLoading.loadClass(metadata).getClassLoader());
            } finally {
                Thread.currentThread().setContextClassLoader(previous);
            }
        }
    }

    @Test
    void resolvesBootLayerModules() {
        try (var scan = new ClassGraph().enableSystemModules().acceptModules("java.base")
                .enableClassInfo().acceptClasses(String.class.getName()).scan()) {
            assertSame(String.class, ClassLoading.loadClass(scan.getClassInfo(String.class.getName())));
        }
    }

    @Test
    void reportsMissingClassesAndIncompatibleTypes() {
        try (var scan = scanFixtures()) {
            var metadata = scan.getClassInfo(Base.class.getName());
            assertThrows(IllegalArgumentException.class, () -> ClassLoading.loadClass(metadata, null));
            assertThrows(ClassCastException.class, () -> ClassLoading.loadClasses(List.of(metadata), Runnable.class));
            ClassLoader missingDependency = new ClassLoader(null) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve) {
                    throw new NoClassDefFoundError("optional/dependency");
                }
            };
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> ClassLoading.loadClass(metadata, missingDependency));
            assertInstanceOf(NoClassDefFoundError.class, failure.getCause());
        }
    }
}
