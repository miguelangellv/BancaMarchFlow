package es.bancamarch.flow.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import es.bancamarch.flow.services.BancaMarchFlowService

class NewTaskAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val taskName = Messages.showInputDialog(
            project,
            "Introduce el nombre de la rama de la nueva tarea:",
            "Nueva Tarea - Banca March Flow",
            null
        ) ?: return

        if (taskName.isBlank()) {
            return Messages.showErrorDialog(project, "El nombre de la rama no puede estar vacío.", "Error")
        }

        val service = BancaMarchFlowService.getInstance(project)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creando tarea...") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Creando ramas para la tarea..."

                val result = service.createNewTask(taskName.trim())

                ApplicationManager.getApplication().invokeLater {
                    result.onSuccess { branches ->
                        Messages.showInfoMessage(
                            project,
                            """Tarea creada correctamente:
                                |• Task: ${branches.taskBranch} (desde origin/master)
                                |• Dev: ${branches.devBranch} (desde origin/develop)
                                |• Test: ${branches.testBranch} (desde origin/test)
                                |
                                |Estás en la rama: ${branches.taskBranch}
                            """.trimMargin(),
                            "Tarea Creada"
                        )
                    }

                    result.onFailure { error ->
                        Messages.showErrorDialog(
                            project,
                            "Error al crear la tarea: ${error.message}",
                            "Error - Banca March Flow"
                        )
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}