package dev.jbang.cli;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;

import dev.jbang.BaseTest;

public class TestArch extends BaseTest {

	@Test
	void testNoDirectSystemEnvAccess() {
		// TODO: how to only import classes that are in the classpath that has dev.jbang
		// in package?
		JavaClasses importedClasses = new ClassFileImporter()
			.withImportOption(new ImportOption() {
				@Override
				public boolean includes(Location target) {
					// only Settings should acccess env
					return !target.contains("dev/jbang/Settings.");
				}
			})
			.importPath("build/classes/java/main/dev/jbang");

		ArchRule rule = noClasses().should()
			.callMethod(System.class, "getenv")
			.orShould()
			.callMethodWhere(target(nameMatching("getProperty"))
				.and(target(owner(assignableTo(System.class)))));

		rule.check(importedClasses);
	}

}
