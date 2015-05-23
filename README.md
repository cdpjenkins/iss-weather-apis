# iss-weather

## Overview

This is a demo to show the use of simple REST APIs from a ClojureScript
program. It helps to answer the question: "if an astronaut (or cosmonaut!) on
the International Space Station were to look directly down at planet Earth,
what weather would they see below them?

It does this by calling two APIs: the first to get the real-time location of
the ISS and then the second to find the weather at that location (albeit 100
miles down, on the surface of the planet). The ISS position and an icon
representing the relevant weather conditions are then shown on an OSM map.

## Usage instructions

Run the demo as follows:

    lein cljsbuild once

    lein ring server
