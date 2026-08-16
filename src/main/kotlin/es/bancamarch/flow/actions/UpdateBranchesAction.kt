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

class UpdateBranchesAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.findCurrentTask()

        if (service.currentTask == null) {
            Messages.showErrorDialog(
                project,
                "No hay tarea activa. Crea una nueva tarea o cambia a una rama que tenga ramas asociadas (task, -to-dev, -to-test).",
                "Error - Banca March Flow"
            )
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Actualizando ramas...") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0
                indicator.text = "Mergeando origin/master en task..."

                val result = service.updateBranches()

                indicator.fraction = 1.0

                ApplicationManager.getApplication().invokeLater {
                    result.onSuccess {
                        Messages.showInfoMessage(
                            project,
                            """Ramas actualizadas correctamente:
                                |• ${service.currentTask!!.taskBranch} ← origin/master
                                |• ${service.currentTask!!.devBranch} ← ${service.currentTask!!.taskBranch}
                                |• ${service.currentTask!!.testBranch} ← ${service.currentTask!!.taskBranch}
                            """.trimMargin(),
                            "Ramas Actualizadas"
                        )
                    }

                    result.onFailure { error ->
                        Messages.showErrorDialog(
                            project,
                            "Error al actualizar ramas: ${error.message}\n\nPuede que haya conflictos de merge. Resuélvelos manualmente.",
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