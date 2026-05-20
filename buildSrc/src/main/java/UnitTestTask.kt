import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named

abstract class UnitTestTask: DefaultTask() {
    init {
        val jvmTestTask = project.tasks.named<Test>("jvmTest")
        jvmTestTask.excludeE2ETests()

        val androidTestTask = project.tasks.named<Test>("testAndroidHostTest")
        androidTestTask.excludeE2ETests()

        jvmTestTask.configure {
            doFirst {
                logger.quiet("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                logger.quiet("Running all unit tests...")
                logger.quiet("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            }
        }

        dependsOn(jvmTestTask, androidTestTask)

        doLast {
            logger.quiet("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            logger.quiet("✓ All unit tests passed")
            logger.quiet("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        }
    }

    private fun TaskProvider<Test>.excludeE2ETests() {
        configure {
            filter {
                excludeTestsMatching("com.arkade.e2e.*")
            }
        }
    }
}