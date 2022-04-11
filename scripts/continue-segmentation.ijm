/**
 * 
 * Fiji ImageJ Macro
 * 
 * Demonstrate Continue Microglia Segmentation and Tracking
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

run("Continue Microglia Segmentation And Tracking", "intensityfile='https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20--t1-5.tif' segmentationfile='https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20-labelMasks--t1-3.tif' relativeintensitythreshold=1.5 outputdirectory='"+outputDirectory+"'");
