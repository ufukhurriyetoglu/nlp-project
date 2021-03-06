package hacettepe.nlp.project.preprocess

import hacettepe.nlp.project.repositories.Repository
import org.apache.commons.lang.StringUtils
import org.apache.logging.log4j.LogManager
import zemberek.morphology.analysis.WordAnalysis
import zemberek.morphology.analysis.tr.TurkishMorphology
import zemberek.tokenization.TurkishTokenizer
import java.io.File
import java.nio.file.Files


/**
 *
 * @author Memn
 * @date 10.04.2018
 *
 */
class PreProcessor {
    val logger = LogManager.getLogger(PreProcessor::class.java.name)

}


fun main(args: Array<String>) {
    val stemmer = PreProcessor()
    Repository.instance.read(false)
    stemmer.logger.info("count movies: ${Repository.instance.movies.size}")
    stemmer.logger.info("count users: ${Repository.instance.users.size}")
    stemmer.logger.info("count posts: ${Repository.instance.movies.values.sumBy { it.posts.size }}")
    stemmer.logger.info("has any post with not existing user: ${Repository.instance.movies.values.any { it.posts.any { !Repository.instance.users.containsKey(it.userId) } }}")
    stemmer.logger.info("count movie with not existing cast: ${Repository.instance.movies.values.count { it.cast.castMembersByType.isEmpty() }}")

    preprocess()


}

private fun preprocess() {

    Files.deleteIfExists(File(Repository.fileName(true, true)).toPath())
    // we need to stem and lemmatize posts
    val tokenizer = TurkishTokenizer.DEFAULT
    val morphology = TurkishMorphology.createWithDefaults()

    Repository.instance.movies.forEach { _, movie ->
        movie.posts.forEach { post ->
            // tokens of user reviews
            val stemmedDescription = ArrayList<String>()
            tokenizer.tokenizeToStrings(post.description).forEach {
                // analyze user post tokens
                if (it.length > 1) {
                    val analysis = morphology.analyze(it).first()
                    when {
                    // nothing to do store as is
                        analysis.isUnknown -> stemmedDescription.add(it)
                    // store lemma instead
                        isNotPunctuation(analysis) -> stemmedDescription.add(analysis.lemma)
                    }
                }
            }
            post.description = StringUtils.join(stemmedDescription.iterator(), '|')
        }
    }
    // we have lemmatized the post-reviews
    Repository.instance.saveStemmedMovies()
}

fun isNotPunctuation(analysis: WordAnalysis): Boolean {
    return analysis.dictionaryItem.primaryPos.shortForm != "Punc"
}
