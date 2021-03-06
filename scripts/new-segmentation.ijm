/**
 * 
 * Fiji ImageJ Macro
 * 
 * Demonstrate Microglia New Segmentation And Tracking
 * 
 * github: https://github.com/embl-cba/microglia-morphometry#microglia-morphometry
 * 
 * Required Update Sites:
 * 
 * - Microglia-Morphometry
 * - IJPB-Plugins
 * 
 **/
 
outputDirectory = getDirectory("temp");

run("New Microglia Segmentation And Tracking", "intensityfile='https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif' relativeintensitythreshold=1.5 outputdirectory='"+outputDirectory+"'");

// to conveniently inspect the output one can double click on the respective lines ("...saved:") in the IJ log window
