package com.example.platform

import com.example.data.local.ActivityEntity
import com.example.data.local.FluxObjectEntity
import com.example.data.local.FluxObjectType
import com.example.data.local.SpaceEntity
import com.example.data.local.TaskItemEntity

data class AssistantResponse(
    val operationTitle: String,
    val summaryText: String,
    val generatedChecklist: List<String> = emptyList(),
    val suggestedTaskTitle: String? = null,
    val suggestedSpaceId: String? = null,
    val matchedObjectTitles: List<String> = emptyList(),
    val disclosureNote: String = "Processed 100% locally on-device by FLUX Local Assistant. Zero data transmitted externally."
)

object LocalAssistantEngine {

    fun summarizeContent(title: String, rawContent: String): AssistantResponse {
        val cleaned = rawContent.trim()
        if (cleaned.isEmpty()) {
            return AssistantResponse(
                operationTitle = "Summary · $title",
                summaryText = "No text content available to summarize. Add note details or text excerpts to generate key takeaways."
            )
        }
        val lines = cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val sentences = cleaned.split(Regex("(?<=[.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.length > 10 }

        val topPoints = when {
            lines.size >= 2 -> lines.take(4)
            sentences.isNotEmpty() -> sentences.take(3)
            else -> listOf(cleaned.take(180))
        }
        val bulletSummary = topPoints.joinToString("\n") { point ->
            "• ${point.removePrefix("-").removePrefix("•").trim()}"
        }

        return AssistantResponse(
            operationTitle = "Concise Summary · $title",
            summaryText = bulletSummary,
            generatedChecklist = topPoints.map { "Review: ${it.take(56)}" }
        )
    }

    fun explainTextOrFormula(title: String, content: String): AssistantResponse {
        val lower = "$title $content".lowercase()
        val explanation = when {
            content.contains("∇") || lower.contains("maxwell") || lower.contains("faraday") ->
                "This expression represents differential electromagnetic field equations:\n" +
                    "1. Faraday's Law (∇ × E = -∂B/∂t): A time-varying magnetic field induces a circulating electric field.\n" +
                    "2. Ampère-Maxwell Law: Conduction current J and displacement current ∂E/∂t generate circulating magnetic fields."
            lower.contains("rlc") || lower.contains("phase") || lower.contains("oscilloscope") ->
                "Resonant circuit telemetry analysis:\n" +
                    "• Phase angle Δφ measures reactive impedance balance between inductance (XL) and capacitance (XC).\n" +
                    "• Higher Q-factor indicates narrower bandwidth and sharper frequency selectivity."
            lower.contains("appwidget") || lower.contains("latency") || lower.contains("spec") ->
                "Android Systems Architecture Breakdown:\n" +
                    "• AppWidgetHost manages out-of-process RemoteViews bound via AppWidgetManager.\n" +
                    "• Frame pacing under 8ms ensures zero dropped frames on 120Hz LTPO panels."
            else ->
                "Structured Breakdown of \"$title\":\n" +
                    "• Primary Concept: ${content.take(120)}\n" +
                    "• Recommended Next Step: Convert actionable clauses into a checklist or pin to an active Space."
        }
        return AssistantResponse(
            operationTitle = "Contextual Explanation",
            summaryText = explanation
        )
    }

    fun generateChecklistFromText(rawText: String): AssistantResponse {
        val rawSegments = rawText
            .split(Regex("[\\n,;•]+"))
            .map { it.replace(Regex("^[-*\\[\\]xX0-9.)\\s]+"), "").trim() }
            .filter { it.length >= 3 }

        val items = if (rawSegments.isNotEmpty()) {
            rawSegments.take(7)
        } else {
            listOf(
                "Review primary source material",
                "Verify key notes and references",
                "Complete remaining action items"
            )
        }

        return AssistantResponse(
            operationTitle = "Generated Action Checklist (${items.size} items)",
            summaryText = "Extracted ${items.size} structured steps ready to insert into your Activity or Study/Work tasks:",
            generatedChecklist = items
        )
    }

    fun parseTaskFromInstruction(
        instruction: String,
        spaces: List<SpaceEntity>
    ): AssistantResponse {
        val cleaned = instruction.trim().removePrefix("create task").removePrefix("remind me to").trim()
        val lower = cleaned.lowercase()
        val matchedSpace = when {
            lower.contains("physics") || lower.contains("study") || lower.contains("exam") ||
                lower.contains("chapter") || lower.contains("paper") ->
                spaces.firstOrNull { it.roleKey == "study" }
            lower.contains("spec") || lower.contains("build") || lower.contains("deploy") ||
                lower.contains("client") || lower.contains("meeting") ->
                spaces.firstOrNull { it.roleKey == "work" }
            lower.contains("call") || lower.contains("message") || lower.contains("mom") ||
                lower.contains("friend") ->
                spaces.firstOrNull { it.roleKey == "people" }
            else -> spaces.firstOrNull { it.roleKey == "personal" } ?: spaces.firstOrNull()
        }

        val taskTitle = cleaned.replaceFirstChar { it.uppercase() }.ifBlank { "Follow up on current activity" }
        return AssistantResponse(
            operationTitle = "Task Drafted Ready to Save",
            summaryText = "Task: \"$taskTitle\"\nAssigned Context: ${matchedSpace?.title ?: "Personal"} Space",
            suggestedTaskTitle = taskTitle,
            suggestedSpaceId = matchedSpace?.id ?: "space_personal"
        )
    }

    fun suggestActionsForObject(obj: FluxObjectEntity): List<String> {
        return when (runCatching { FluxObjectType.valueOf(obj.objectType) }.getOrDefault(FluxObjectType.NOTE)) {
            FluxObjectType.SCREENSHOT -> listOf(
                "Extract measurement text & convert to Note",
                "Attach to Physics Revision or Work Activity",
                "Share via Android Sharesheet",
                "Apply as Procedural/System Wallpaper"
            )
            FluxObjectType.DOCUMENT -> listOf(
                "Open in compatible PDF/Document reader",
                "Generate revision checklist from summary",
                "Pin reference to Study Space",
                "Share document URI"
            )
            FluxObjectType.LINK -> listOf(
                "Open URL in browser",
                "Copy link to clipboard",
                "Save reference to active Activity",
                "Summarize topic context"
            )
            FluxObjectType.NOTE, FluxObjectType.CHECKLIST -> listOf(
                "Summarize key takeaways",
                "Extract actionable tasks into checklist",
                "Explain technical concepts",
                "Share text to external app"
            )
            else -> listOf(
                "Open object target",
                "Move to another Space",
                "Add to Continuity Activity",
                "Share via system sheet"
            )
        }
    }

    fun locateInLocalIndex(
        query: String,
        activities: List<ActivityEntity>,
        objects: List<FluxObjectEntity>,
        tasks: List<TaskItemEntity>
    ): AssistantResponse {
        val tokens = query.lowercase().split(" ").filter { it.length > 2 }
        if (tokens.isEmpty()) {
            return AssistantResponse(
                operationTitle = "FLUX Index Locator",
                summaryText = "Enter keywords (e.g., 'Maxwell', 'Kyoto', 'oscilloscope', 'widget') to locate matching Activities, Objects, and Tasks."
            )
        }
        val actMatches = activities.filter { act ->
            val hay = "${act.title} ${act.summary}".lowercase()
            tokens.any { hay.contains(it) }
        }.map { "Activity: ${it.title} (${it.progressPercent}%)" }

        val objMatches = objects.filter { obj ->
            val hay = "${obj.title} ${obj.subtitle} ${obj.uriOrTarget}".lowercase()
            tokens.any { hay.contains(it) }
        }.map { obj -> "${obj.objectType}: ${obj.title}" }

        val taskMatches = tasks.filter { t ->
            val hay = t.title.lowercase()
            tokens.any { hay.contains(it) }
        }.map { t -> "Task: ${t.title} (${if (t.isDone) "Done" else t.dueLabel})" }

        val combined = (actMatches + objMatches + taskMatches).take(8)
        val summary = if (combined.isEmpty()) {
            "No local items matched \"$query\". Try broader keywords or create a new Activity."
        } else {
            "Found ${combined.size} local matches across your FLUX environment:\n" +
                combined.joinToString("\n") { "• $it" }
        }
        return AssistantResponse(
            operationTitle = "Local Index Results (${combined.size})",
            summaryText = summary,
            matchedObjectTitles = combined
        )
    }
}
