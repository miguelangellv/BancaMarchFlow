package es.bancamarch.flow.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import git4idea.repo.GitRepositoryManager
import es.bancamarch.flow.services.BancaMarchFlowService

class BranchChangeStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val service = BancaMarchFlowService.getInstance(project)

        // Detectar la rama actual al iniciar
        val repoManager = GitRepositoryManager.getInstance(project)
        val repo = repoManager.repositories.firstOrNull()
        repo?.currentBranch?.name?.let { currentBranch ->
            service.detectTaskFromBranch(currentBranch)
        }

        // Escuchar cambios futuros de rama
        val connection = project.messageBus.connect()
        connection.subscribe(GitRepository.GIT_REPO_CHANGE, GitRepositoryChangeListener { repository ->
            val branch = repository.currentBranch?.name ?: return@GitRepositoryChangeListener
            service.detectTaskFromBranch(branch)
        })
    }
}
