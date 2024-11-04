package com.suwinel.statisticsplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.DefaultCategoryDataset
import javax.swing.BoxLayout
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextArea
import java.awt.Dimension

class MyToolbarButtonAction : AnAction("StatisticsButton") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.dataContext.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR) ?: return
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return

        val fileLines = editor.document.lineCount
        val longestFunction = findLongestFunction(psiFile, editor)
        val todoCount = countTodos(psiFile)

        val textInfo = generateTextInfo(fileLines, longestFunction, todoCount)
        val dataset = createDataset(fileLines, longestFunction?.second, todoCount)

        showStatisticsDialog("File Statistics", textInfo, dataset)
    }

    private fun findLongestFunction(psiFile: com.intellij.psi.PsiFile, editor: com.intellij.openapi.editor.Editor): Pair<String?, Int>? {
        val functions = PsiTreeUtil.collectElements(psiFile) {
            it.elementType?.debugName == "FUN"
        }

        return functions.map {
            val function = it
            val offset = function.textOffset
            val endOffset = function.endOffset
            val firstLine = editor.document.getLineNumber(offset)
            val endLine = editor.document.getLineNumber(endOffset)
            val length = endLine - firstLine
            function.text.substringBefore("(").trim() to length
        }.maxByOrNull { it.second }
    }

    private fun countTodos(psiFile: com.intellij.psi.PsiFile): Int {
        val comments = PsiTreeUtil.collectElements(psiFile) {
            it.elementType?.debugName == "EOL_COMMENT"
        }
        return comments.count { it.text.contains(Regex("TODO", RegexOption.IGNORE_CASE)) }
    }

    private fun generateTextInfo(totalLines: Int, longestFunction: Pair<String?, Int>?, todoCount: Int): String {
        val longestFunctionName = longestFunction?.first
        val longestFunctionLength = longestFunction?.second
        return """
            Total Lines: $totalLines
            Longest Function: $longestFunctionName ($longestFunctionLength lines)
            TODO Count: $todoCount
        """.trimIndent()
    }

    private fun createDataset(totalLines: Int, longestFunctionLength: Int?, todoCount: Int): DefaultCategoryDataset {
        val dataset = DefaultCategoryDataset()
        dataset.addValue(totalLines, "Lines", "Total Lines")
        dataset.addValue(longestFunctionLength ?: 0, "Lines", "Longest Function")
        dataset.addValue(todoCount, "Count", "TODOs")
        return dataset
    }

    private fun showStatisticsDialog(title: String, textInfo: String, dataset: DefaultCategoryDataset) {
        val chart: JFreeChart = ChartFactory.createBarChart(
            title,
            "Category",
            "Value",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        )

        val chartPanel = ChartPanel(chart).apply { preferredSize = Dimension(400, 300) }
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JTextArea(textInfo).apply { isEditable = false })
            add(chartPanel)
        }

        JOptionPane.showMessageDialog(
            null,
            panel,
            title,
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}
