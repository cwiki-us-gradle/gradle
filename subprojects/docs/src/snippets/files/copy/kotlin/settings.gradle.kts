// tag::change-default-exclusions[]
import org.apache.tools.ant.DirectoryScanner

DirectoryScanner.removeDefaultExclude("**/.git")
DirectoryScanner.removeDefaultExclude("**/.git/**")
// end::change-default-exclusions[]
rootProject.name = "copy"
