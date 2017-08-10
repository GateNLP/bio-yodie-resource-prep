import java.text.Normalizer
import java.text.Normalizer.Form
import java.util.Locale
import org.rogach.scallop._;

// This is a preliminary version for normalizing labels
// The program reads from a TSV file and does a number of normalizations
// of the string in column N, then outputs optionally the original line
// plus one ore more normalized versions.
// This is parametrized by language because some kinds of normalization need
// to be specific to one language, e.g. the suffix "_(Begriffserklärung)" 
// can get removed from disambiguation labels in German.



object NormalizeLabels {
  def main(args: Array[String]) = {
    
    
    class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
      val processAnchors = opt[Boolean]("processAnchors",'a',descr="Different behavior when processing anchor text instead of labels",required=false,default=Some(false))
      val colnr = trailArg[Int](required=true,descr="Column number of label column (0-based)")
      val lang = trailArg[String](required=true,descr="Language of the labels")
    }
    val conf = new Conf(args)
    
    
    val colnr = conf.colnr()
    val lang = conf.lang()
    val processAnchors = conf.processAnchors()
    var nlines = 0
    var ndropped = 0
    var nadded = 0
    var nwritten = 0
    var nerror = 0
    
    val patternTrailingParenthesis = """\s*\(\s*([^)]+)\s*\)\s*$""".r
    val patternAfterFirstComma = """,\s+(.+)\s*$""".r
    val containsLetter = """\p{L}""".r
    val containsUmlaut = """[üöäÜÖÄ]""".r
    
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      if(cols.size <= colnr) {
        System.err.println("NormalizeLabels: Not at least "+(colnr+1)+" columns in line "+nlines+": >"+line+"<")
        nerror += 1
      } else {
        var label = cols(colnr)
        
        var toEmit = List[String]()
        
        label = Normalizer.normalize(label,Form.NFKC)
        
        var parenthesesInfo = "NONE"
        
        // NOTE: original this removed trailing parentheses or stuff after
        // a comma, but only if processAnchors was false.
        // We now always process trailing Parentheses. 
        var matched = patternTrailingParenthesis.findFirstMatchIn(label).orNull
        if(matched != null) {
          parenthesesInfo = matched.group(1)
          label = patternTrailingParenthesis.replaceAllIn(label,"")
        }
        
        // remove trailing comma from the label, this is usually a bug
        // Not sure about trailing "?" or other punctuation??
        label = label.trim()
        label = label.replaceAll(",$","")
        label = label.trim() 
        // Correct some odd patterns we sometimes find
        // Replace html escapes
        label = label.replaceAll("""&quot;""","\"")
        // remove quotes around the full label
        label = label.replaceAll(""""(.+)"""","$1")
        label = label.replaceAll("""''(.+)''""","$1")
        
        
        // This cleaned label is now used as the "original label" string
        var labelOrig = label
        
        
        // if we have no letters left in the label, skip, so if we find a letter
        // lets go on
        // also, if the length is 1, lets skip, so if > 1, lets go on
        if(label.size > 1 && label.size <= 50 && containsLetter.findFirstIn(label).nonEmpty) {
          // convert to lower case 
          // TODO: adapt to Gazetteer normalization / pipeline normalization!!!
          // TODO: make sure we use the correct locale, depending on language!
          // TODO: make the lower-casing optional and/or dependent on the 
          //   kind of label (e.g. do not do this for abbreviations?)
          label = label.toLowerCase(new Locale(lang))
          
          // we now know we want to keep that label
          toEmit = label :: toEmit 
          
          // if the label contains accented characters, add a line with the 
          // non-accented characters
          var labelNoAccent = 
          Normalizer.normalize(label, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
          labelNoAccent = Normalizer.normalize(labelNoAccent,Form.NFKC)
          if(!label.equals(labelNoAccent)) {
            toEmit = labelNoAccent :: toEmit
            nadded += 1
          }
          
          // TODO: do Umlaut substitution on all the labels we want to emit
          // (first approx: only on label, since it is unlikely to have both 
          // umlauts and accented characters in one word
          if(containsUmlaut.findFirstIn(label).nonEmpty) {
            val sb = new StringBuffer()
            // TODO: check if this will handle non char characters correctly???
            label.foreach { c => 
              sb.append(
              c match {
                case 'ä' => "ae"
                case 'ö' => "oe"
                case 'ü' => "ue"
                case 'Ä' => "AE"
                case 'Ö' => "OE"
                case 'Ü' => "UE"
                case _ => c.toString
              })
            }
            toEmit = sb.toString :: toEmit
            nadded += 1
          }
          
          // Output the rows with any additional label variations we have created. 
          toEmit.foreach { newLabel => 
            cols(colnr) = newLabel
            println(cols.mkString("\t")+"\t"+labelOrig+"\t"+parenthesesInfo)
            nwritten += 1
          }
          
        } else {
          ndropped += 1
        }
        
        if((nlines % 100000) == 0) {
          System.err.println("NormalizeLabels - processed: "+nlines)
        }
      }
    }
    System.err.println("NormalizeLabels: Completed")
    System.err.println("NormalizeLabels: total lines: "+nlines)    
    System.err.println("NormalizeLabels: dropped:     "+ndropped)
    System.err.println("NormalizeLabels: added:       "+nadded)
    System.err.println("NormalizeLabels: written:     "+nwritten)
    System.err.println("NormalizeLabels: errors:      "+nerror)
  }
}
