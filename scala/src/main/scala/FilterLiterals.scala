import java.text.Normalizer
import java.text.Normalizer.Form

// Filter the literals we use for RDF-based similarity.
// At the moment this does the following:
// = remove all whitespace-separated tokens which do not consist of just letters
// = remove repeated, leading and trailing whitespace
// = if what remains is empty, skip the whole row, otherwise output the changed row

object FilterLiterals {
  def main(args: Array[String]) = {
    
    if(args.size != 1) {
      System.err.println("parms: literal-column-number (0-based)")
      System.exit(1)
    }
    val colnr = args(0).toInt    
    
    var nlines = 0
    var ndropped = 0
    var nerror = 0
    var nwritten = 0
    
    val patternPunctuation = """[~=+%£"'`|@#,;\.\?!:()\[\]\{\}\<\>&*\$/\\„“]+"""
    val patternNotAllLetters = """\b\S*[\S&&[^\p{L}-]]\S*\b"""
    val patternMultipleSpaces = """\s\s+"""
    val patternSingleHyphens = """(?<!\p{L})-(?!\p{L})"""
    
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      
      if(cols.size <= colnr) {
        System.err.println("FilterLiterals: Not at least "+(colnr+1)+" columns in line "+nlines+": >"+line+"<")
        nerror += 1
      } else {
        var literal = cols(colnr)
        val orig = literal
        literal = literal.replaceAll(patternPunctuation, " ")
        literal = literal.replaceAll(patternNotAllLetters, " ")
        literal = literal.replaceAll(patternSingleHyphens, " ")
        literal = literal.replaceAll(patternMultipleSpaces, " ")
        literal = literal.trim()
        if(literal.isEmpty()) {
          //System.err.println("dropped: "+orig)
          ndropped = ndropped + 1
        } else {
          //System.err.println("was: "+orig)
          //System.err.println("now: "+literal)
          cols(colnr) = literal
          println(cols.mkString("\t"))
          nwritten += 1
        }
        if((nlines % 100000) == 0) {
          System.err.println("FilterLiterals - processed: "+nlines)
        }
      }
    }
    System.err.println("FilterLiterals: Completed")
    System.err.println("FilterLiterals: total lines: "+nlines)    
    System.err.println("FilterLiterals: dropped:     "+ndropped)
    System.err.println("FilterLiterals: written:     "+nwritten)
    System.err.println("FilterLiterals: errors:      "+nerror)
  }
}
