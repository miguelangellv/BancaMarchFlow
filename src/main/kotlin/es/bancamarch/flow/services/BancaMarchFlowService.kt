package es.bancamarch.flow.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import git4idea.branch.GitBrancher
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

@Service(Service.Level.PROJECT)
class BancaMarchFlowService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): BancaMarchFlowService = project.service()
    }

    data class TaskBranches(
        val taskBranch: String,
        val devBranch: String,
        val testBranch: String
    )

    var currentTask: TaskBranches? = null
        private set

    fun getRepository(): GitRepository? {
        val manager = GitRepositoryManager.getInstance(project)
        return manager.repositories.firstOrNull()
    }

    fun getCurrentBranchName(): String? {
        return getRepository()?.currentBranch?.name
    }

    fun createNewTask(taskName: String): Result<TaskBranches> = runCatching {
        val repo = getRepository() ?: return Result.failure(Exception("No se encontró repositorio Git"))

        val devBranch = "$taskName-to-dev"
        val testBranch = "$taskName-to-test"

        // Fetch para tener las ramas remotas actualizadas
        runGitFetch(repo)

        // Crear rama task desde origin/master
        runGitCheckout(repo, "-b", taskName, "origin/master")

        // Crear rama to-dev desde origin/develop
        runGitCheckout(repo, "-b", devBranch, "origin/develop")

        // Crear rama to-test desde origin/test
        runGitCheckout(repo, "-b", testBranch, "origin/test")

        // Volver a la rama task
        runGitCheckout(repo, taskName)

        // Refrescar el repositorio y el VFS
        refreshRepository(repo)

        currentTask = TaskBranches(taskName, devBranch, testBranch)
        currentTask!!
    }

    fun updateBranches(): Result<Unit> = runCatching {
        val repo = getRepository() ?: throw Exception("No se encontró repositorio Git")
        val task = currentTask ?: throw Exception("No hay tarea activa. Selecciona o crea una tarea primero.")

        // Fetch
        runGitFetch(repo)

        // 1. Merge master en task
        runGitCheckout(repo, task.taskBranch)
        runGitMerge(repo, "origin/master")

        // 2. Merge task en to-dev
        runGitCheckout(repo, task.devBranch)
        runGitMerge(repo, task.taskBranch)

        // 3. Merge task en to-test
        runGitCheckout(repo, task.testBranch)
        runGitMerge(repo, task.taskBranch)

        // Volver a la rama task
        runGitCheckout(repo, task.taskBranch)

        // Refrescar el repositorio y el VFS
        refreshRepository(repo)

    }

    fun pushAllBranches(): Result<Unit> = runCatching {
        val repo = getRepository() ?: throw Exception("No se encontró repositorio Git")
        val task = currentTask ?: throw Exception("No hay tarea activa.")
        runGitPush(repo, task.taskBranch)
        runGitPush(repo, task.devBranch)
        runGitPush(repo, task.testBranch)
        repo.update()
    }

    fun switchToBranch(branchName: String): Result<Unit> = runCatching {
        val repo = getRepository() ?: return Result.failure(Exception("No se encontró repositorio Git"))
        val brancher = GitBrancher.getInstance(project)
        brancher.checkout(branchName, false, listOf(repo)) {}

    }

    fun detectTaskFromBranch(branchName: String): TaskBranches? {
        val repo = getRepository() ?: return null
        val localBranches = repo.branches.localBranches.map { it.name }

        val baseName = branchName.removeSuffix("-to-dev").removeSuffix("-to-test")
        val devBranch = "$baseName-to-dev"
        val testBranch = "$baseName-to-test"

        if (baseName !in localBranches) {
            return null
        }

        if (devBranch !in localBranches) {
            return null
        }

        if (testBranch !in localBranches) {
            return null
        }

        val branches = TaskBranches(baseName, devBranch, testBranch)
        currentTask = branches
        return branches
    }

    private fun refreshRepository(repo: GitRepository) {
        repo.update()
        VfsUtil.markDirtyAndRefresh(false, true, true, repo.root)
    }

    private fun runGitFetch(repo: GitRepository) {
        val handler = GitLineHandler(project, repo.root, GitCommand.FETCH)
        handler.addParameters("origin")
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            throw Exception("Git fetch falló: ${result.errorOutputAsJoinedString}")
        }
    }

    private fun runGitCheckout(repo: GitRepository, vararg params: String) {
        val handler = GitLineHandler(project, repo.root, GitCommand.CHECKOUT)
        params.forEach { handler.addParameters(it) }
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            throw Exception("Git checkout falló: ${result.errorOutputAsJoinedString}")
        }
    }

    private fun runGitMerge(repo: GitRepository, branch: String) {
        val handler = GitLineHandler(project, repo.root, GitCommand.MERGE)
        handler.addParameters(branch)
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            throw Exception("Git merge de '$branch' falló: ${result.errorOutputAsJoinedString}")
        }
    }

    private fun runGitPush(repo: GitRepository, branchName: String) {
        val handler = GitLineHandler(project, repo.root, GitCommand.PUSH)
        handler.addParameters("origin", branchName)
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            throw Exception("Git push de '$branchName' falló: ${result.errorOutputAsJoinedString}")
        }
    }

    fun findCurrentTask(): TaskBranches? {
        return currentTask ?: getCurrentBranchName()?.let { branchName -> detectTaskFromBranch(branchName) }
    }
}
