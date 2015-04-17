// Code generated by dagger-compiler.  Do not edit.
package br.com.estudio89.syncing.security;

import dagger.MembersInjector;
import dagger.internal.Binding;
import dagger.internal.Linker;
import java.util.Set;

/**
 * A {@code Binding<SecurityUtil>} implementation which satisfies
 * Dagger's infrastructure requirements including:
 *
 * Owning the dependency links between {@code SecurityUtil} and its
 * dependencies.
 *
 * Being a {@code Provider<SecurityUtil>} and handling creation and
 * preparation of object instances.
 *
 * Being a {@code MembersInjector<SecurityUtil>} and handling injection
 * of annotated fields.
 */
public final class SecurityUtil$$InjectAdapter extends Binding<SecurityUtil>
    implements MembersInjector<SecurityUtil> {
  private Binding<br.com.estudio89.syncing.SyncConfig> syncConfig;

  public SecurityUtil$$InjectAdapter() {
    super(null, "members/br.com.estudio89.syncing.security.SecurityUtil", NOT_SINGLETON, SecurityUtil.class);
  }

  /**
   * Used internally to link bindings/providers together at run time
   * according to their dependency graph.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void attach(Linker linker) {
    syncConfig = (Binding<br.com.estudio89.syncing.SyncConfig>) linker.requestBinding("br.com.estudio89.syncing.SyncConfig", SecurityUtil.class, getClass().getClassLoader());
  }

  /**
   * Used internally obtain dependency information, such as for cyclical
   * graph detection.
   */
  @Override
  public void getDependencies(Set<Binding<?>> getBindings, Set<Binding<?>> injectMembersBindings) {
    injectMembersBindings.add(syncConfig);
  }

  /**
   * Injects any {@code @Inject} annotated fields in the given instance,
   * satisfying the contract for {@code Provider<SecurityUtil>}.
   */
  @Override
  public void injectMembers(SecurityUtil object) {
    object.syncConfig = syncConfig.get();
  }

}
