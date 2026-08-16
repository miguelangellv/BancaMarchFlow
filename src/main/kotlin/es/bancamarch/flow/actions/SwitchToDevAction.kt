package es.bancamarch.flow.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import es.bancamarch.flow.services.BancaMarchFlowService

class SwitchToDevAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.currentTask ?: return Messages.showErrorDialog(project, "No hay tarea activa.", "Error - Banca March Flow")

        service.switchToBranch(task.devBranch).onFailure { error ->
            Messages.showErrorDialog(project, "Error: ${error.message}", "Error - Banca March Flow")
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.currentTask
        e.presentation.isEnabled = task != null
        e.presentation.text = if (task != null) "Dev: ${task.devBranch}" else "Ir a To-Dev"
    }
}