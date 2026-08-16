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

class PushAllBranchesAction : AnAction() {


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.findCurrentTask() ?: return Messages.showErrorDialog(
                project,
                "No hay tarea activa. Crea una nueva tarea o cambia a una rama que tenga ramas asociadas.",
                "Error - Banca March Flow"
            )

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Pusheando ramas...") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                val result = service.pushAllBranches()

                ApplicationManager.getApplication().invokeLater {
                    result.onSuccess {
                        Messages.showInfoMessage(
                            project,
                            """Push completado correctamente:
                                |• ${task.taskBranch}
                                |• ${task.devBranch}
                                |• ${task.testBranch}
                            """.trimMargin(),
                            "Push Completado"
                        )
                    }

                    result.onFailure { error ->
                        Messages.showErrorDialog(
                            project,
                            "Error al hacer push: ${error.message}",
                            "Error - Banca March Flow"
                        )
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val service = BancaMarchFlowService.getInstance(project)
        e.presentation.isEnabled = service.currentTask != null
    }
}

