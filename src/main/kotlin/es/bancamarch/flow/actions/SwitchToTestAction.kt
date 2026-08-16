package es.bancamarch.flow.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import es.bancamarch.flow.services.BancaMarchFlowService

class SwitchToTestAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.currentTask

        if (task == null) {
            Messages.showErrorDialog(project, "No hay tarea activa.", "Error - Banca March Flow")
            return
        }

        service.switchToBranch(task.testBranch).onFailure { error ->
            Messages.showErrorDialog(project, "Error: ${error.message}", "Error - Banca March Flow")
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.currentTask
        e.presentation.isEnabled = task != null
        e.presentation.text = if (task != null) "Test: ${task.testBranch}" else "Ir a To-Test"
    }
}