package org.gradle.accessors.dm;

import org.jspecify.annotations.NullMarked;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.AttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;
import org.gradle.api.GradleException;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NullMarked
public class LibrariesForLibsInPluginsBlock extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final AuthlibLibraryAccessors laccForAuthlibLibraryAccessors = new AuthlibLibraryAccessors(owner);
    private final ConstantLibraryAccessors laccForConstantLibraryAccessors = new ConstantLibraryAccessors(owner);
    private final FxLibraryAccessors laccForFxLibraryAccessors = new FxLibraryAccessors(owner);
    private final HelloLibraryAccessors laccForHelloLibraryAccessors = new HelloLibraryAccessors(owner);
    private final JavaLibraryAccessors laccForJavaLibraryAccessors = new JavaLibraryAccessors(owner);
    private final JetbrainsLibraryAccessors laccForJetbrainsLibraryAccessors = new JetbrainsLibraryAccessors(owner);
    private final JnaLibraryAccessors laccForJnaLibraryAccessors = new JnaLibraryAccessors(owner);
    private final JunitLibraryAccessors laccForJunitLibraryAccessors = new JunitLibraryAccessors(owner);
    private final KalaLibraryAccessors laccForKalaLibraryAccessors = new KalaLibraryAccessors(owner);
    private final LwjglLibraryAccessors laccForLwjglLibraryAccessors = new LwjglLibraryAccessors(owner);
    private final MonetLibraryAccessors laccForMonetLibraryAccessors = new MonetLibraryAccessors(owner);
    private final NayukiLibraryAccessors laccForNayukiLibraryAccessors = new NayukiLibraryAccessors(owner);
    private final PciLibraryAccessors laccForPciLibraryAccessors = new PciLibraryAccessors(owner);
    private final SimpleLibraryAccessors laccForSimpleLibraryAccessors = new SimpleLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibsInPluginsBlock(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, AttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Dependency provider for <b>chardet</b> with <b>org.glavo:chardet</b> coordinates and
     * with version reference <b>chardet</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getChardet() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>fxsvgimage</b> with <b>com.github.hervegirod:fxsvgimage</b> coordinates and
     * with version reference <b>fxsvgimage</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getFxsvgimage() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>gson</b> with <b>com.google.code.gson:gson</b> coordinates and
     * with version reference <b>gson</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getGson() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>hmclauncher</b> with <b>org.glavo.hmcl:HMCLauncher</b> coordinates and
     * with version reference <b>hmclauncher</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getHmclauncher() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>jimfs</b> with <b>com.google.jimfs:jimfs</b> coordinates and
     * with version reference <b>jimfs</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getJimfs() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>jsoup</b> with <b>org.jsoup:jsoup</b> coordinates and
     * with version reference <b>jsoup</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getJsoup() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>jwebp</b> with <b>org.glavo:webp</b> coordinates and
     * with version reference <b>jwebp</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getJwebp() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>lz4</b> with <b>org.glavo:lz4-java</b> coordinates and
     * with version reference <b>lz4</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getLz4() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>nanohttpd</b> with <b>org.nanohttpd:nanohttpd</b> coordinates and
     * with version reference <b>nanohttpd</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getNanohttpd() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>tomlj</b> with <b>org.tomlj:tomlj</b> coordinates and
     * with version reference <b>tomlj</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getTomlj() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>weburl</b> with <b>org.glavo:weburl</b> coordinates and
     * with version reference <b>weburl</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getWeburl() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>xz</b> with <b>org.tukaani:xz</b> coordinates and
     * with version reference <b>xz</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getXz() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>authlib</b>
     */
    public AuthlibLibraryAccessors getAuthlib() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>constant</b>
     */
    public ConstantLibraryAccessors getConstant() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>fx</b>
     */
    public FxLibraryAccessors getFx() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>hello</b>
     */
    public HelloLibraryAccessors getHello() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>java</b>
     */
    public JavaLibraryAccessors getJava() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>jetbrains</b>
     */
    public JetbrainsLibraryAccessors getJetbrains() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>jna</b>
     */
    public JnaLibraryAccessors getJna() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>junit</b>
     */
    public JunitLibraryAccessors getJunit() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>kala</b>
     */
    public KalaLibraryAccessors getKala() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>lwjgl</b>
     */
    public LwjglLibraryAccessors getLwjgl() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>monet</b>
     */
    public MonetLibraryAccessors getMonet() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>nayuki</b>
     */
    public NayukiLibraryAccessors getNayuki() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>pci</b>
     */
    public PciLibraryAccessors getPci() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>simple</b>
     */
    public SimpleLibraryAccessors getSimple() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class AuthlibLibraryAccessors extends SubDependencyFactory {

        public AuthlibLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>injector</b> with <b>org.glavo.hmcl:authlib-injector</b> coordinates and
         * with version reference <b>authlib.injector</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getInjector() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class ConstantLibraryAccessors extends SubDependencyFactory {
        private final ConstantPoolLibraryAccessors laccForConstantPoolLibraryAccessors = new ConstantPoolLibraryAccessors(owner);

        public ConstantLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>constant.pool</b>
         */
        public ConstantPoolLibraryAccessors getPool() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class ConstantPoolLibraryAccessors extends SubDependencyFactory {

        public ConstantPoolLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>scanner</b> with <b>org.jenkins-ci:constant-pool-scanner</b> coordinates and
         * with version reference <b>constant.pool.scanner</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getScanner() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class FxLibraryAccessors extends SubDependencyFactory {

        public FxLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>gson</b> with <b>org.hildan.fxgson:fx-gson</b> coordinates and
         * with version reference <b>fx.gson</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getGson() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class HelloLibraryAccessors extends SubDependencyFactory {

        public HelloLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>nbt</b> with <b>org.glavo:HelloNBT</b> coordinates and
         * with version reference <b>hello.nbt</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getNbt() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class JavaLibraryAccessors extends SubDependencyFactory {

        public JavaLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>info</b> with <b>org.glavo:java-info</b> coordinates and
         * with version reference <b>java.info</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getInfo() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class JetbrainsLibraryAccessors extends SubDependencyFactory {

        public JetbrainsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>annotations</b> with <b>org.jetbrains:annotations</b> coordinates and
         * with version reference <b>jetbrains.annotations</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAnnotations() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class JnaLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public JnaLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>jna</b> with <b>net.java.dev.jna:jna</b> coordinates and
         * with version reference <b>jna</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> asProvider() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>platform</b> with <b>net.java.dev.jna:jna-platform</b> coordinates and
         * with version reference <b>jna</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getPlatform() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class JunitLibraryAccessors extends SubDependencyFactory {

        public JunitLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>jupiter</b> with <b>org.junit.jupiter:junit-jupiter</b> coordinates and
         * with version reference <b>junit</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJupiter() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class KalaLibraryAccessors extends SubDependencyFactory {
        private final KalaCompressLibraryAccessors laccForKalaCompressLibraryAccessors = new KalaCompressLibraryAccessors(owner);

        public KalaLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>kala.compress</b>
         */
        public KalaCompressLibraryAccessors getCompress() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class KalaCompressLibraryAccessors extends SubDependencyFactory {

        public KalaCompressLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>ar</b> with <b>org.glavo.kala:kala-compress-archivers-ar</b> coordinates and
         * with version reference <b>kala.compress</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAr() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>tar</b> with <b>org.glavo.kala:kala-compress-archivers-tar</b> coordinates and
         * with version reference <b>kala.compress</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTar() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>zip</b> with <b>org.glavo.kala:kala-compress-archivers-zip</b> coordinates and
         * with version reference <b>kala.compress</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getZip() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class LwjglLibraryAccessors extends SubDependencyFactory {
        private final LwjglUnsafeLibraryAccessors laccForLwjglUnsafeLibraryAccessors = new LwjglUnsafeLibraryAccessors(owner);

        public LwjglLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>lwjgl.unsafe</b>
         */
        public LwjglUnsafeLibraryAccessors getUnsafe() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class LwjglUnsafeLibraryAccessors extends SubDependencyFactory {

        public LwjglUnsafeLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>agent</b> with <b>org.glavo:lwjgl-unsafe-agent</b> coordinates and
         * with version reference <b>lwjgl.unsafe.agent</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAgent() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class MonetLibraryAccessors extends SubDependencyFactory {

        public MonetLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>fx</b> with <b>org.glavo:MonetFX</b> coordinates and
         * with version reference <b>monet.fx</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getFx() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class NayukiLibraryAccessors extends SubDependencyFactory {

        public NayukiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>qrcodegen</b> with <b>io.nayuki:qrcodegen</b> coordinates and
         * with version reference <b>nayuki.qrcodegen</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getQrcodegen() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class PciLibraryAccessors extends SubDependencyFactory {

        public PciLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>ids</b> with <b>org.glavo:pci-ids</b> coordinates and
         * with version reference <b>pci.ids</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getIds() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class SimpleLibraryAccessors extends SubDependencyFactory {
        private final SimplePngLibraryAccessors laccForSimplePngLibraryAccessors = new SimplePngLibraryAccessors(owner);

        public SimpleLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>simple.png</b>
         */
        public SimplePngLibraryAccessors getPng() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class SimplePngLibraryAccessors extends SubDependencyFactory {

        public SimplePngLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>javafx</b> with <b>org.glavo:simple-png-javafx</b> coordinates and
         * with version reference <b>simple.png.javafx</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJavafx() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        private final AuthlibVersionAccessors vaccForAuthlibVersionAccessors = new AuthlibVersionAccessors(providers, config);
        private final ConstantVersionAccessors vaccForConstantVersionAccessors = new ConstantVersionAccessors(providers, config);
        private final FxVersionAccessors vaccForFxVersionAccessors = new FxVersionAccessors(providers, config);
        private final HelloVersionAccessors vaccForHelloVersionAccessors = new HelloVersionAccessors(providers, config);
        private final JavaVersionAccessors vaccForJavaVersionAccessors = new JavaVersionAccessors(providers, config);
        private final JetbrainsVersionAccessors vaccForJetbrainsVersionAccessors = new JetbrainsVersionAccessors(providers, config);
        private final KalaVersionAccessors vaccForKalaVersionAccessors = new KalaVersionAccessors(providers, config);
        private final LwjglVersionAccessors vaccForLwjglVersionAccessors = new LwjglVersionAccessors(providers, config);
        private final MonetVersionAccessors vaccForMonetVersionAccessors = new MonetVersionAccessors(providers, config);
        private final NayukiVersionAccessors vaccForNayukiVersionAccessors = new NayukiVersionAccessors(providers, config);
        private final PciVersionAccessors vaccForPciVersionAccessors = new PciVersionAccessors(providers, config);
        private final SimpleVersionAccessors vaccForSimpleVersionAccessors = new SimpleVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>chardet</b> with value <b>2.5.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getChardet() { return getVersion("chardet"); }

        /**
         * Version alias <b>fxsvgimage</b> with value <b>1.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getFxsvgimage() { return getVersion("fxsvgimage"); }

        /**
         * Version alias <b>gson</b> with value <b>2.13.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getGson() { return getVersion("gson"); }

        /**
         * Version alias <b>hmclauncher</b> with value <b>3.7.0.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getHmclauncher() { return getVersion("hmclauncher"); }

        /**
         * Version alias <b>jimfs</b> with value <b>1.3.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJimfs() { return getVersion("jimfs"); }

        /**
         * Version alias <b>jna</b> with value <b>5.18.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJna() { return getVersion("jna"); }

        /**
         * Version alias <b>jsoup</b> with value <b>1.21.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJsoup() { return getVersion("jsoup"); }

        /**
         * Version alias <b>junit</b> with value <b>6.0.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJunit() { return getVersion("junit"); }

        /**
         * Version alias <b>jwebp</b> with value <b>0.2.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJwebp() { return getVersion("jwebp"); }

        /**
         * Version alias <b>lz4</b> with value <b>1.10.4.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLz4() { return getVersion("lz4"); }

        /**
         * Version alias <b>nanohttpd</b> with value <b>2.3.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getNanohttpd() { return getVersion("nanohttpd"); }

        /**
         * Version alias <b>shadow</b> with value <b>9.3.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getShadow() { return getVersion("shadow"); }

        /**
         * Version alias <b>terracotta</b> with value <b>0.4.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getTerracotta() { return getVersion("terracotta"); }

        /**
         * Version alias <b>tomlj</b> with value <b>1.1.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getTomlj() { return getVersion("tomlj"); }

        /**
         * Version alias <b>weburl</b> with value <b>0.2.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getWeburl() { return getVersion("weburl"); }

        /**
         * Version alias <b>xz</b> with value <b>1.12</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getXz() { return getVersion("xz"); }

        /**
         * Group of versions at <b>versions.authlib</b>
         */
        public AuthlibVersionAccessors getAuthlib() {
            return vaccForAuthlibVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.constant</b>
         */
        public ConstantVersionAccessors getConstant() {
            return vaccForConstantVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.fx</b>
         */
        public FxVersionAccessors getFx() {
            return vaccForFxVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.hello</b>
         */
        public HelloVersionAccessors getHello() {
            return vaccForHelloVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.java</b>
         */
        public JavaVersionAccessors getJava() {
            return vaccForJavaVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.jetbrains</b>
         */
        public JetbrainsVersionAccessors getJetbrains() {
            return vaccForJetbrainsVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.kala</b>
         */
        public KalaVersionAccessors getKala() {
            return vaccForKalaVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.lwjgl</b>
         */
        public LwjglVersionAccessors getLwjgl() {
            return vaccForLwjglVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.monet</b>
         */
        public MonetVersionAccessors getMonet() {
            return vaccForMonetVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.nayuki</b>
         */
        public NayukiVersionAccessors getNayuki() {
            return vaccForNayukiVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.pci</b>
         */
        public PciVersionAccessors getPci() {
            return vaccForPciVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.simple</b>
         */
        public SimpleVersionAccessors getSimple() {
            return vaccForSimpleVersionAccessors;
        }

    }

    public static class AuthlibVersionAccessors extends VersionFactory  {

        public AuthlibVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>authlib.injector</b> with value <b>1.2.7</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getInjector() { return getVersion("authlib.injector"); }

    }

    public static class ConstantVersionAccessors extends VersionFactory  {

        private final ConstantPoolVersionAccessors vaccForConstantPoolVersionAccessors = new ConstantPoolVersionAccessors(providers, config);
        public ConstantVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.constant.pool</b>
         */
        public ConstantPoolVersionAccessors getPool() {
            return vaccForConstantPoolVersionAccessors;
        }

    }

    public static class ConstantPoolVersionAccessors extends VersionFactory  {

        public ConstantPoolVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>constant.pool.scanner</b> with value <b>1.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getScanner() { return getVersion("constant.pool.scanner"); }

    }

    public static class FxVersionAccessors extends VersionFactory  {

        public FxVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>fx.gson</b> with value <b>5.0.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getGson() { return getVersion("fx.gson"); }

    }

    public static class HelloVersionAccessors extends VersionFactory  {

        public HelloVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>hello.nbt</b> with value <b>0.3.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getNbt() { return getVersion("hello.nbt"); }

    }

    public static class JavaVersionAccessors extends VersionFactory  {

        public JavaVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>java.info</b> with value <b>1.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getInfo() { return getVersion("java.info"); }

    }

    public static class JetbrainsVersionAccessors extends VersionFactory  {

        public JetbrainsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>jetbrains.annotations</b> with value <b>26.1.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getAnnotations() { return getVersion("jetbrains.annotations"); }

    }

    public static class KalaVersionAccessors extends VersionFactory  {

        public KalaVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>kala.compress</b> with value <b>1.27.1-3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCompress() { return getVersion("kala.compress"); }

    }

    public static class LwjglVersionAccessors extends VersionFactory  {

        private final LwjglUnsafeVersionAccessors vaccForLwjglUnsafeVersionAccessors = new LwjglUnsafeVersionAccessors(providers, config);
        public LwjglVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.lwjgl.unsafe</b>
         */
        public LwjglUnsafeVersionAccessors getUnsafe() {
            return vaccForLwjglUnsafeVersionAccessors;
        }

    }

    public static class LwjglUnsafeVersionAccessors extends VersionFactory  {

        public LwjglUnsafeVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>lwjgl.unsafe.agent</b> with value <b>2.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getAgent() { return getVersion("lwjgl.unsafe.agent"); }

    }

    public static class MonetVersionAccessors extends VersionFactory  {

        public MonetVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>monet.fx</b> with value <b>0.4.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getFx() { return getVersion("monet.fx"); }

    }

    public static class NayukiVersionAccessors extends VersionFactory  {

        public NayukiVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>nayuki.qrcodegen</b> with value <b>1.8.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getQrcodegen() { return getVersion("nayuki.qrcodegen"); }

    }

    public static class PciVersionAccessors extends VersionFactory  {

        public PciVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>pci.ids</b> with value <b>0.4.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getIds() { return getVersion("pci.ids"); }

    }

    public static class SimpleVersionAccessors extends VersionFactory  {

        private final SimplePngVersionAccessors vaccForSimplePngVersionAccessors = new SimplePngVersionAccessors(providers, config);
        public SimpleVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.simple.png</b>
         */
        public SimplePngVersionAccessors getPng() {
            return vaccForSimplePngVersionAccessors;
        }

    }

    public static class SimplePngVersionAccessors extends VersionFactory  {

        public SimplePngVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>simple.png.javafx</b> with value <b>0.3.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJavafx() { return getVersion("simple.png.javafx"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, AttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>shadow</b> with plugin id <b>com.gradleup.shadow</b> and
         * with version reference <b>shadow</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getShadow() { return createPlugin("shadow"); }

    }

}
