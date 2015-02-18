// Code generated by dagger-compiler.  Do not edit.
package br.com.estudio89.syncing.injection;

import dagger.internal.Binding;
import dagger.internal.BindingsGroup;
import dagger.internal.Linker;
import dagger.internal.ModuleAdapter;
import dagger.internal.ProvidesBinding;
import java.util.Set;
import javax.inject.Provider;

/**
 * A manager of modules and provides adapters allowing for proper linking and
 * instance provision of types served by {@code @Provides} methods.
 */
public final class DataSyncHelperModule$$ModuleAdapter extends ModuleAdapter<DataSyncHelperModule> {
  private static final String[] INJECTS = { "members/br.com.estudio89.syncing.DataSyncHelper", "members/br.com.estudio89.syncing.CustomTransactionManager", "members/br.com.estudio89.syncing.ServerComm", "members/br.com.estudio89.syncing.SyncConfig", "members/br.com.estudio89.syncing.bus.AsyncBus", "members/br.com.estudio89.syncing.ThreadChecker", "members/br.com.estudio89.syncing.extras.ServerAuthenticate", };
  private static final Class<?>[] STATIC_INJECTIONS = { };
  private static final Class<?>[] INCLUDES = { };

  public DataSyncHelperModule$$ModuleAdapter() {
    super(br.com.estudio89.syncing.injection.DataSyncHelperModule.class, INJECTS, STATIC_INJECTIONS, false /*overrides*/, INCLUDES, false /*complete*/, false /*library*/);
  }

  @Override
  public DataSyncHelperModule newModule() {
    return new br.com.estudio89.syncing.injection.DataSyncHelperModule();
  }

  /**
   * Used internally obtain dependency information, such as for cyclical
   * graph detection.
   */
  @Override
  public void getBindings(BindingsGroup bindings, DataSyncHelperModule module) {
    bindings.contributeProvidesBinding("br.com.estudio89.syncing.CustomTransactionManager", new ProvideCustomTransactionManagerProvidesAdapter(module));
    bindings.contributeProvidesBinding("br.com.estudio89.syncing.ServerComm", new ProvideServerCommProvidesAdapter(module));
    bindings.contributeProvidesBinding("br.com.estudio89.syncing.SyncConfig", new ProvideSyncConfigProvidesAdapter(module));
    bindings.contributeProvidesBinding("br.com.estudio89.syncing.bus.AsyncBus", new ProvideBusProvidesAdapter(module));
    bindings.contributeProvidesBinding("br.com.estudio89.syncing.ThreadChecker", new ProvideThreadCheckerProvidesAdapter(module));
  }

  /**
   * A {@code Binding<br.com.estudio89.syncing.CustomTransactionManager>} implementation which satisfies
   * Dagger's infrastructure requirements including:
   *
   * Being a {@code Provider<br.com.estudio89.syncing.CustomTransactionManager>} and handling creation and
   * preparation of object instances.
   */
  public static final class ProvideCustomTransactionManagerProvidesAdapter extends ProvidesBinding<br.com.estudio89.syncing.CustomTransactionManager>
      implements Provider<br.com.estudio89.syncing.CustomTransactionManager> {
    private final DataSyncHelperModule module;

    public ProvideCustomTransactionManagerProvidesAdapter(DataSyncHelperModule module) {
      super("br.com.estudio89.syncing.CustomTransactionManager", NOT_SINGLETON, "br.com.estudio89.syncing.injection.DataSyncHelperModule", "provideCustomTransactionManager");
      this.module = module;
      setLibrary(false);
    }

    /**
     * Returns the fully provisioned instance satisfying the contract for
     * {@code Provider<br.com.estudio89.syncing.CustomTransactionManager>}.
     */
    @Override
    public br.com.estudio89.syncing.CustomTransactionManager get() {
      return module.provideCustomTransactionManager();
    }
  }

  /**
   * A {@code Binding<br.com.estudio89.syncing.ServerComm>} implementation which satisfies
   * Dagger's infrastructure requirements including:
   *
   * Being a {@code Provider<br.com.estudio89.syncing.ServerComm>} and handling creation and
   * preparation of object instances.
   */
  public static final class ProvideServerCommProvidesAdapter extends ProvidesBinding<br.com.estudio89.syncing.ServerComm>
      implements Provider<br.com.estudio89.syncing.ServerComm> {
    private final DataSyncHelperModule module;

    public ProvideServerCommProvidesAdapter(DataSyncHelperModule module) {
      super("br.com.estudio89.syncing.ServerComm", NOT_SINGLETON, "br.com.estudio89.syncing.injection.DataSyncHelperModule", "provideServerComm");
      this.module = module;
      setLibrary(false);
    }

    /**
     * Returns the fully provisioned instance satisfying the contract for
     * {@code Provider<br.com.estudio89.syncing.ServerComm>}.
     */
    @Override
    public br.com.estudio89.syncing.ServerComm get() {
      return module.provideServerComm();
    }
  }

  /**
   * A {@code Binding<br.com.estudio89.syncing.SyncConfig>} implementation which satisfies
   * Dagger's infrastructure requirements including:
   *
   * Owning the dependency links between {@code br.com.estudio89.syncing.SyncConfig} and its
   * dependencies.
   *
   * Being a {@code Provider<br.com.estudio89.syncing.SyncConfig>} and handling creation and
   * preparation of object instances.
   */
  public static final class ProvideSyncConfigProvidesAdapter extends ProvidesBinding<br.com.estudio89.syncing.SyncConfig>
      implements Provider<br.com.estudio89.syncing.SyncConfig> {
    private final DataSyncHelperModule module;
    private Binding<android.content.Context> context;
    private Binding<br.com.estudio89.syncing.bus.AsyncBus> bus;

    public ProvideSyncConfigProvidesAdapter(DataSyncHelperModule module) {
      super("br.com.estudio89.syncing.SyncConfig", IS_SINGLETON, "br.com.estudio89.syncing.injection.DataSyncHelperModule", "provideSyncConfig");
      this.module = module;
      setLibrary(false);
    }

    /**
     * Used internally to link bindings/providers together at run time
     * according to their dependency graph.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void attach(Linker linker) {
      context = (Binding<android.content.Context>) linker.requestBinding("android.content.Context", DataSyncHelperModule.class, getClass().getClassLoader());
      bus = (Binding<br.com.estudio89.syncing.bus.AsyncBus>) linker.requestBinding("br.com.estudio89.syncing.bus.AsyncBus", DataSyncHelperModule.class, getClass().getClassLoader());
    }

    /**
     * Used internally obtain dependency information, such as for cyclical
     * graph detection.
     */
    @Override
    public void getDependencies(Set<Binding<?>> getBindings, Set<Binding<?>> injectMembersBindings) {
      getBindings.add(context);
      getBindings.add(bus);
    }

    /**
     * Returns the fully provisioned instance satisfying the contract for
     * {@code Provider<br.com.estudio89.syncing.SyncConfig>}.
     */
    @Override
    public br.com.estudio89.syncing.SyncConfig get() {
      return module.provideSyncConfig(context.get(), bus.get());
    }
  }

  /**
   * A {@code Binding<br.com.estudio89.syncing.bus.AsyncBus>} implementation which satisfies
   * Dagger's infrastructure requirements including:
   *
   * Being a {@code Provider<br.com.estudio89.syncing.bus.AsyncBus>} and handling creation and
   * preparation of object instances.
   */
  public static final class ProvideBusProvidesAdapter extends ProvidesBinding<br.com.estudio89.syncing.bus.AsyncBus>
      implements Provider<br.com.estudio89.syncing.bus.AsyncBus> {
    private final DataSyncHelperModule module;

    public ProvideBusProvidesAdapter(DataSyncHelperModule module) {
      super("br.com.estudio89.syncing.bus.AsyncBus", IS_SINGLETON, "br.com.estudio89.syncing.injection.DataSyncHelperModule", "provideBus");
      this.module = module;
      setLibrary(false);
    }

    /**
     * Returns the fully provisioned instance satisfying the contract for
     * {@code Provider<br.com.estudio89.syncing.bus.AsyncBus>}.
     */
    @Override
    public br.com.estudio89.syncing.bus.AsyncBus get() {
      return module.provideBus();
    }
  }

  /**
   * A {@code Binding<br.com.estudio89.syncing.ThreadChecker>} implementation which satisfies
   * Dagger's infrastructure requirements including:
   *
   * Being a {@code Provider<br.com.estudio89.syncing.ThreadChecker>} and handling creation and
   * preparation of object instances.
   */
  public static final class ProvideThreadCheckerProvidesAdapter extends ProvidesBinding<br.com.estudio89.syncing.ThreadChecker>
      implements Provider<br.com.estudio89.syncing.ThreadChecker> {
    private final DataSyncHelperModule module;

    public ProvideThreadCheckerProvidesAdapter(DataSyncHelperModule module) {
      super("br.com.estudio89.syncing.ThreadChecker", IS_SINGLETON, "br.com.estudio89.syncing.injection.DataSyncHelperModule", "provideThreadChecker");
      this.module = module;
      setLibrary(false);
    }

    /**
     * Returns the fully provisioned instance satisfying the contract for
     * {@code Provider<br.com.estudio89.syncing.ThreadChecker>}.
     */
    @Override
    public br.com.estudio89.syncing.ThreadChecker get() {
      return module.provideThreadChecker();
    }
  }
}