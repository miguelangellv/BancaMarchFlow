package es.bancamarch.flow.widget

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import es.bancamarch.flow.services.BancaMarchFlowService
import java.awt.event.MouseEvent

class FlowBranchWidget(private val project: Project) : StatusBarWidget,
    StatusBarWidget.MultipleTextValuesPresentation {

    private var statusBar: StatusBar? = null

    override fun ID(): String = "BancaMarchFlowWidget"

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText(): String = "Banca March Flow - Cambiar rama"

    override fun getSelectedValue(): String? {
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.currentTask ?: return "BM Flow: Sin tarea"
        val current = service.getCurrentBranchName() ?: return "BM Flow: ${task.taskBranch}"

        return when (current) {
            task.taskBranch -> "BM: ★ ${task.taskBranch}"
            task.devBranch -> "BM: → ${task.devBranch}"
            task.testBranch -> "BM: → ${task.testBranch}"
            else -> "BM Flow: ${task.taskBranch}"
        }
    }

    override fun getPopup(): ListPopup? {
        val service = BancaMarchFlowService.getInstance(project)
        val task = service.currentTask ?: return null

        val branches = listOf(
            "★ Task: ${task.taskBranch}" to task.taskBranch,
            "→ Dev: ${task.devBranch}" to task.devBranch,
            "→ Test: ${task.testBranch}" to task.testBranch
        )

        val step = object : BaseListPopupStep<Pair<String, String>>(
            "Banca March Flow - Cambiar rama", branches
        ) {
            override fun getTextFor(value: Pair<String, String>): String = value.first

            override fun onChosen(selectedValue: Pair<String, String>, finalChoice: Boolean): PopupStep<*>? {
                service.switchToBranch(selectedValue.second)
                return FINAL_CHOICE
            }
        }

        return JBPopupFactory.getInstance().createListPopup(step)
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? = null

    fun update() {
        statusBar?.updateWidget(ID())
    }
}

