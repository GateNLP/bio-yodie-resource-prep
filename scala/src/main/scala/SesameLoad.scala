// TBD: fast and efficient way to load a large file into a Sesame/OWLIM reposiroty
// without makign the server time out or otherwise misbehave.

// Main idea: if the input is Turtle/NTriples use the Sesame RIO parser

// = get a connection
// = set AutoCommit to false
// = create a parser for the input format which uses our own handler
// = parse the input
// = commit
// = close connection
// The handler: takes connection and chunk size as init parameter
// = uses an RDFInserter 
// = in the constructor, create a new RDFInserter(conn)
// = keeps a counter of how many we have inserted
// = passes on startRDF, endRDF, handleNamespace, handleComment(?)
// = handleStatement(Statement): pass on to inserter, count, if
//   we have exceeded the chunk size, commit
// See also http://rivuli-development.com/further-reading/sesame-cookbook/loading-large-file-in-sesame-native/
object SesameLoad {
  
}
