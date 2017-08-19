# DocBook XInclude-based chunking helper

## Natural content splitting
If large documents are maintained, it is handy to split them into smaller parts. Such content is not only easier to navigate but it also brings other benefits, especially if the source files are versioned using systems like SVN or Git. If documents are edited by multiple users, there is a lower risk of conflicts when integrating changes back into the main branch. It is also easier to track particular changes in the version history. 

Unfortunately, currently there are no means to preserve this natural splitting when generating set of HTML pages. 

## Current chunking methods
When generating HTML outputs, the original XML file can be split (chunked) into individual output files using two basic methods:

 1. **automatic chunking** - in simple terms all chapters and sections up to the specified depth produce separate HTML files
 2. **manually controlled chunking** - separate HTML files are produced based on the configuration file containing the final structure with IDs which match IDs in the source document

While the latter method allows to completely control the splitting process, it is only useful for documents with the stable structure as it requires hand editing. This is the main reason why the first method is still preferred, even though the result is suboptimal. By default the first section is kept together with its parent. This may me confusing as first sections do not produce separate files, but other sections do. However, if the first section would be split separately, the parent chunk could contain just the title.

## XInclude-based chunking
Once document is split naturally, why not reuse this division also for chunking? Good news. It is not a problem any more when keeping several simple rules and employing this handy tool!

### Rules
All rules are demostrated in the example below. 

`main.xml`
```xml
<book xmlns="http://docbook.org/ns/docbook"
      xmlns:xi="http://www.w3.org/2001/XInclude"
      version="5.0">
    <?dbhtml xinclude-based-chunking?> (2)
    <xi:include href="info.xml"> (1b)
        <?dbhtml do-not-chunk-this-xinclude?> (3)
    </xi:include>
    <chapter xml:id="natural-content-splitting"> (1a) (5)
        <title>Natural content splitting</title>
        <para>content</para>
    </chapter>
    <chapter xml:id="current-chunking-methods"> (1a) (5)
        <title>Current chunking methods</title>
        <para>content</para>
    </chapter>
    <chapter xml:id="xinclude-based-chunking"> (5)
        <title>XInclude-based chunking</title>
        <xi:include href="rules.xml"/> (1b)
    </chapter>
</book>
```
`bookinfo.xml`
```xml
<info xmlns="http://docbook.org/ns/docbook" version="5.0">
    <title>DocBook HTML outputs - managed splitting into multiple files</title>
</info>
```
`rules.xml`
```xml
<section xmlns="http://docbook.org/ns/docbook" 
         xmlns:xi="http://www.w3.org/2001/XInclude" 
         version="5.0" 
         xml:id="rules"> (4)
    <title>Rules</title>
    <xi:include href="rule-list.xml"/> (1c)
</section>
```
`rule-list.xml`
```xml
<orderedlist xmlns="http://docbook.org/ns/docbook" version="5.0">
    <listitem>
        <para>listitem</para>
     </listitem>
</orderedlist>
```

 1. Create a main XML document the content of which is either (a) in-place (embedded) or (b) linked via XIncludes. Keep in mind (c) nested XIncludes in linked files have no influence on chunking.
 2. Add `<?dbhtml xinclude-based-chunking?>` processing instruction into the main document to enable the XInclude-based chunking.
 3. Optionally add `<?dbhtml do-not-chunk-this-xinclude?>` processing instruction to the body of XInclude element to disable creating a separate HTML file for the referenced content.
 4. Ensure all XIncluded parts have an `id` attribute on the root element.
 5. Ensure all embedded parts (chapters, sections) intended to chunk have their `id` attribute.

If the above XML source is transformed into HTML, see the next section, you should see 5 separate files in the output folder:

 - index.html
 - natural-content-splitting.html
 - current-chunking-methods.html
 - xinclude-based-chunking.html
 - rules.html

### Generating HTML output

 1. Creating configuration file for chunking
      1. download or build the tool from sources 
      2. run `java -jar docbook-xinclude-based-chunking-helper-{version}.jar -xml:main.xml`
     
     This will create `toc.xml` file which is later used for custom chunking. If there are parent nodes with all children included via XIncludes, the local table of contents will be generated to avoid cases when the particular file contains just the title without any additional content.

 2. Resolving XIncludes
     
     While it can be integrated into XSLT transformation step, because of nasty Xerces bug I prefer resolving XIncludes in a separate step, using a different tool:
     1. download `xmllint` tool for your operating system at http://xmlsoft.org/downloads.html
     2. run `xmllint --xinclude -o main-resolved.xml main.xml`

 3. Performing the XSLT transformation
    
    Besides the source XML file and XSLT template (actually, a slightly customized built-in DocBook template for manual chunking) we have to pass three additional parameters to XSLT processor:
    - `base.dir` - output folder path
    - `chunk.toc` - explicit Table of Contents (to be used for chunking)
    - `manual.toc` - explicit Table of Contents
    
    For both `chunk.toc` and `manual.toc` the generated `toc.xml` file needs to be specified.
    
    An example command for [Saxon 6.5.5](http://saxon.sourceforge.net/saxon6.5.5/ "Saxon 6.5.5") XSLT processor on Windows operating system:
   
    ```Batchfile
    java -cp C:\DocBook\tools\saxon.jar ^
    com.icl.saxon.StyleSheet ^
    main-resolved.xml ^
    C:\xslt\chunktoc.xsl ^
    base.dir=C:/example/output/ ^
    chunk.toc=file:///C:/example/toc.xml ^
    manual.toc=file:///C:/example/toc.xml
    ```

## How to build

 - Clone this repository to your local disc.
 - Ensure that JDK 8 is available on your system.
 - Open this Maven based project in your favorite IDE.
 - Build the project.

The final jar file is located in the `target` subfolder.

## How to use

For usage just run the tool in console without any parameters:
`java -jar docbook-xinclude-based-chunking-helper-{version}.jar`
