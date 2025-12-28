package war.metaphor;

import war.configuration.ConfigurationSection;
import war.jar.JarReader;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.data.integer.IntegerTableMutator;
import war.metaphor.mutator.data.integer.SaltingIntegerMutator;
import war.metaphor.mutator.data.strings.LightStringMutator;
import war.metaphor.mutator.data.strings.StringMutator;
import war.metaphor.mutator.data.strings.poly2.NewStringMutator;
import war.metaphor.mutator.flow.*;
import war.metaphor.mutator.integrity.CallGraphIntegrityMutator;
import war.metaphor.mutator.integrity.mainCallCheck.MainCallCheckMutator;
import war.metaphor.mutator.integrity.method.MethodIntegrityMutator;
import war.metaphor.mutator.loader.CleanupMutator;
import war.metaphor.mutator.loader.IndyMutator;
import war.metaphor.mutator.loader.IntegrateLoaderMutator;
import war.metaphor.mutator.loader.MultiNewArrayMutator;
import war.metaphor.mutator.misc.*;
import war.metaphor.mutator.optimization.OptimizationMutator;
import war.metaphor.mutator.optimization.UnusedClassMutator;
import war.metaphor.mutator.optimization.UnusedMethodMutator;
import war.metaphor.mutator.parameter.ExchangeMutator;
import war.metaphor.mutator.ref.ReferenceMutator;
import war.metaphor.mutator.runtime.RuntimePatchMutator;
import war.metaphor.mutator.splash.SplashScreenMutator;
import war.metaphor.mutator.virtualization.VirtualizingMutator;

import java.nio.file.Path;

public class Metaphor {

    public ObfuscatorContext buildObfuscatePass(JarReader intake, ConfigurationSection cfg, String dir) {
        return ObfuscatorContext.builder()
                .input(intake.getInput().toPath())
                .output(Path.of(String.format("%s/metaphor-temp.jar", dir)))
                .mappings(cfg.getStringList("mappings"))
                .section("mutators.metaphor")
                .config(cfg)
                .classes(intake.getClasses())
                .libraries(intake.getLibraries())
                .resources(intake.getResources())
                .manifest(intake.getManifest())

                .mutator("method-call-fix", MethodCallFixer.class)
                .mutator("bootstrap-entry", BootstrapEntryMutator.class)

                .mutator("unused-method-remover", UnusedMethodMutator.class)
                .mutator("unused-class-remover", UnusedClassMutator.class)

                .mutator("optimizer", OptimizationMutator.class)
                .mutator("inlining", MethodInliningMutator.class)
                .mutator("field-initialize", FieldInlinerMutator.class)
                .mutator("access-unify", AccessUnifyMutator.class)

                .mutator("internal-class-integrator", InternalClassIntegrateMutator.class)

                .mutator("renamer.class", ClassRenameMutator.class)
                .mutator("renamer.method", MethodRenameMutator.class)
                .mutator("renamer.field", FieldRenameMutator.class)
                .mutator("renamer.desc", DescriptorMutator.class)

                .mutator("main-call-check", MainCallCheckMutator.class)
                .mutator("call-graph", CallGraphIntegrityMutator.class)
                .mutator("method-integrity", MethodIntegrityMutator.class)

                .mutator("string.poly", StringMutator.class)
                .mutator("string.poly2", NewStringMutator.class)
                .mutator("string.light", LightStringMutator.class)
                .mutator("flow.break", BlockBreakMutator.class)
                .mutator("flow.flattening", ControlFlowFlatteningMutator.class)
                .mutator("flow.shuffle", InstructionShuffleMutator.class)
                .mutator("flow.switch", SwitchMutator.class)
                .mutator("flow.traps", TrapEdgeMutator.class)
                .mutator("flow.opaque", OpaquePredicatesMutator.class)
                .mutator("number.salt", SaltingIntegerMutator.class)
                .mutator("number.table", IntegerTableMutator.class)

                .mutator("ref", ReferenceMutator.class)

                .mutator("lift-constructors", LiftInitializersMutator.class)

                .mutator("watermark", WatermarkMutator.class)

                .mutator("strip", StripMutator.class)

                .mutator("dot-graph", DotExportMutator.class)

                .mutator("virtualize", VirtualizingMutator.class)

                .mutator("indy-rewriter", IndyMutator.class)

                .mutator("splash-screen", SplashScreenMutator.class)

                //.mutator("goto-to-jsr", GotoToJsrMutator.class)
                .mutator("array-rewriter", MultiNewArrayMutator.class)

                .mutator("runtime-patch", RuntimePatchMutator.class)
                .mutator("exchange", ExchangeMutator.class)
                .build();
    }

    public ObfuscatorContext buildPackagePass(JarReader intake, ConfigurationSection cfg, String dir) {
        return ObfuscatorContext.builder()
                .input(intake.getInput().toPath())
                .output(Path.of(String.format("%s/output-final.jar", dir)))
                .section("mutators.jnt")
                .config(cfg)
                .classes(intake.getClasses())
                .libraries(intake.getLibraries())
                .resources(intake.getResources())
                .manifest(intake.getManifest())
                .mutator("cleanup", CleanupMutator.class)
                .mutator("integrate", IntegrateLoaderMutator.class)
                .build();
    }
}
