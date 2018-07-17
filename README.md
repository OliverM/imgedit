# imgedit

An implementation of the OnTheMarket code test, by Oliver Mooney 

## Installation
The project is implemented in Clojure, using Leiningen.

If neither is installed on your machine, install:
1. A current JVM (via https://java.com/en/download/ or using a commandline package manager).
2. Leiningen (via the instructions at https://leiningen.org)

Once both are installed, clone the project at the command line with 
`git clone https://github.com/OliverM/imgedit.git`

This will install the source into the imgedit directory.

## Usage

In the project directory, type `lein run` at the command line to launch the interface.

To run tests, in the project directory type `lein test`.

## Implementation discussion
I implemented the front-end parser using Instaparse. I hadn't used Instaparse previously and wanted to gain some experience using it. On the whole it went smoothly but there is an overlap between its post-parsing functionality and spec's `conform` functionality, so in general I didn't use the latter as widely as I might otherwise.

The requirements are implemented in a layered fashion; the parser limits numeric dimensions to positive integers beginning with 1, while the specs for dimension values limit them to 1-250, the image-command spec ensures commands fall within image boundaries, and the fdef generators for the inner implementation commands ensure commands are generated images with compatible boundaries.

## Assumptions & Ambiguities
- The spec stipulates arguments are separated by a single space; for ease of use I've relaxed this to one or more spaces
- The spec says nothing about communication of parsing errors, so I've stuck to Instaparse's default for this, and leveraged it further by insisting numbers begin with 1 at the parsing level, rather than accepting numbers like "04"
- The spec doesn't constrain the set of colours beyond single characters, so I'm just using `char?` as a validation of the parsed value
- The spec says nothing about the starting state of the program, so I'm initialising it to a default state of a blank 10 x 10 image
