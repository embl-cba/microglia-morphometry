/**
 * 
 * Fiji ImageJ Macro
 * 
 * Demonstrate Microglia Morphometry Measurements
 * 
 * github: https://github.com/embl-cba/microglia-morphometry#microglia-morphometry
 * 
 * Required Update Sites:
 * 
 * - Microglia-Morphometry
 * - IJPB-Plugins
 * 
 **/


// open input images
open("https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif");
open("https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20--t1-3-labelMasks.tif");

outputDirectory = getDirectory("temp");

// perform and save measurements
run("Measure Microglia Morphometry", "intensityfile='https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif' labelmaskfile='https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20--t1-3-labelMasks.tif' outputdirectory='"+outputDirectory+"'");

// to conveniently inspect the output one can double click on the respective lines ("...saved:") in the IJ log window