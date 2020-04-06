package sk.scednote.model.data

data class Lesson (
    val id: Long,
    val day: Day,
    val time: IntRange,
    val sort: ScedSort = ScedSort.FREE,
    val subject: Subject? = null,
    val room: String = ""
)