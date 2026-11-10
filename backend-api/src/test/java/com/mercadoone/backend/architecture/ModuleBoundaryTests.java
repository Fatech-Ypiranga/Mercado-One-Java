package com.mercadoone.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Um modulo usa outro apenas pela camada {@code application} (services e seus records) e por entidades de
 * {@code domain} referenciadas em relacoes JPA. Repositories e controllers sao internos ao modulo.
 */
class ModuleBoundaryTests {

    private static final String MODULES = "com.mercadoone.backend.modules";
    private static final List<String> NAMES = List.of(
            "access", "audit", "catalog", "customer", "inventory", "offline", "sales", "supplier", "system");

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(MODULES);

    @Test
    void modulesDoNotUseOtherModulesInfrastructureOrApi() {
        for (String module : NAMES) {
            noClasses().that().resideInAPackage(MODULES + "." + module + "..")
                    .should().dependOnClassesThat(
                            com.tngtech.archunit.base.DescribedPredicate.describe(
                                    "infrastructure/api de outro modulo",
                                    javaClass -> javaClass.getPackageName().startsWith(MODULES + ".")
                                            && !javaClass.getPackageName().startsWith(MODULES + "." + module)
                                            && (javaClass.getPackageName().endsWith(".infrastructure")
                                            || javaClass.getPackageName().endsWith(".api"))))
                    .check(classes);
        }
    }
}
