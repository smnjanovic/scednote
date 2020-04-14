package sk.scednote.model.data

data class Subject(val id: Long?, val abb: String, val full: String) {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if(other !is Subject) return false
        return abb == other.abb && full == other.full
    }
}