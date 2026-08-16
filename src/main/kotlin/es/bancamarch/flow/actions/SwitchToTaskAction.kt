package es.bancamarch.flow.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import es.bancamarch.flow.services.BancaMarchFlowService

class SwitchToTaskAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.currentTask

        if (task == null) {
            Messages.showErrorDialog(project, "No hay tarea activa.", "Error - Banca March Flow")
            return
        }

        service.switchToBranch(task.taskBranch).onFailure { error ->
            Messages.showErrorDialog(project, "Error: ${error.message}", "Error - Banca March Flow")
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.currentTask
        e.presentation.isEnabled = task != null
        e.presentation.text = if (task != null) "Task: ${task.taskBranch}" else "Ir a Task"
    }
}