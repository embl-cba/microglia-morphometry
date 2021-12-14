# Microglia Morphometry

A Fiji plugin for semi-automated segmentation, tracking and morphometric analysis of microglia cells in 2D images.

## Install

- Fiji
- Updates sites:
    - IJPB-Plugins 
    - ...

## Run

## Microglia segmentation and tracking

The microglia segmentation and tracking is preformed semi-automated.

Input:
- Single color TIFF stack time lapse with microglia signal, each TIFF plane corresponding to one time point
    - [Example intensity input](https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif)

Output:
- TIFF stack with label mask images, each TIFF plane corrsponding to one time point. Cells keep their label across time, thereby encoding the tracking results.
    - [Example label mask output](https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20-labelMasks--t1-3.tif)

### Parameters

There are several parameters that can be set to adapt the algorithm to different cell types. Below are the values that were used for this publication. 

- `minimal_microglia_size` = 200 micrometer^2
- `skel_maxLength` = 450 micrometer

### Automated conversion to binary masks

For all time points, the images are smoothed using an anisotropic diffusion filter [TODO: REF] and then individually binarised by means of an intensity threshold that is determined automatically from the respective image intensity histogram: The intensity at the histogram mode (`intensity_mode`) is computed as well as the right hand side (towards higher intensities) intensity where the count decreases to half the count at the mode (`intensity_rightHandHalfMode`). The threshold is then computed as `intensity_mode + 1.5 *( intensity_rightHandHalfBin - intensity_mode )`. On the resulting binary image connected components smaller than `minimal_microglia_size` are removed.

### Semi-automated instance segmentation and tracking

The instance segmentation (object detection) and tracking are linked and preformed with human interaction. The main challenge here is that microliga can be touching making it difficult to separate them fully automatically. 

In brieg, for each time point, the binary mask is first subjected to an automated object splitting algorithm. Next, there is the opportunity to manually paint 

#### Automated object splitting

The binary masks are converted to label masks using connected component analysis. Each connected component is skeletonized and the total skeleton length (`skel_length`) is measured. A likely number of microglia within each connected component is computed as `n = ceil( skel_length / skel_maxLength)`. For each connected component with `n > 1` the position of local intensity maxima (corresponding to likely microglia soma) is computed. The `n` brightest local intensity maxima are used as seed points to split the connected component using an intensity based watershed algorithm. Splits are only accepted if the resulting objects are larger than `minimal_microglia_size`.

### Manual label mask correction 

For each time point, the result of above automated steps are presented to the user for manual correction. The user sees the intensity image and the label mask image side by side and the ability to draw into the label mask image. Using the ImageJ ROI tools one can select a region and either set pixels to 0 or 1. Clicking the "Update labels" button will then first convert the corrected label mask image to a binary image and then recompute a new label mask image. Thus, in effect, setting pixels to 0 can be used to split or remove (parts of) cells and settings pixels to 1 can be used to join or add cells.

TODO: Link to video

### Automated tracking and splitting

Once the above manual segmentation correction is finished the user clicks the "Next frame" button. This will trigger the tracking of cells from the current frame (`t`) to the next frame (`t+1`). In frame `t+1` the algorithm starts from the automatically segmented cells as explained above. Next, it takes into account the (manually corrected) information from the current frame `t` to split cells that are likely wrongly connected in frame `t+1`. To do so, for each connected component in frame `t+1` it determines the potentially multiple label regions that it overlaps with in frame `t`. Using those overlap regions as seeds it performs a watershed splitting to draw dividing lines in frame `t+1`. Using the resulting binary mask image it then assigns the labels in frame `t+1` based on the label that each connected component maximally overlaps with in frame `t`. If the overlap is zero, a new label index will be assigned. This way of assinging the label indices in frame `t+1` implements the tracking such that cells belonging to one track have the same label index throughout the time series. 

### Saving the label mask images

The output of the segmentation and tracking are a label mask TIFF stack, with each TIFF plane corresponding to one time point. This label mask stack can be saved by clicking the "Save" button. The "Stop and save" button also terminates the program and closes the user interface windows.

## Microglia morphometry

The intensity and corresonding label mask images are used to compute shape and intensity features for each segmented cell.

Input:
- Single color TIFF stack time lapse with microglia signal, each TIFF plane corresponding to one time point
    - [Example intensity input](https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif)
- TIFF stack with label mask images, each TIFF plane corrsponding to one time point. Cells keeping their label across time, thereby encoding the tracking results .
    - [Example label mask output](https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg6-3CF1_20-labelMasks--t1-3.tif)

Output:
- CSV file containing various cell shape and intensity features (see below for details)
    - [Example cell features CSV](https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/output/MAX_pg6-3CF1_20-labelMasks--t1-3.tif.csv)
- TIFF stack with point annotation images, each TIFF plane corrsponding to one time point.
    - [Example annotation output](https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/output/MAX_pg6-3CF1_20-labelMasks--t1-3.tif-annotations.tif)
- TIFF stack with cell skeleton images, each TIFF plane corrsponding to one time point.
    - [Example skeleton output](https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/output//MAX_pg6-3CF1_20-labelMasks--t1-3.tif-skeletons.tif)

## Microglia morphometry features

Units nomenclature:
- _Frames: time point
- _Pixel: length in pixel units
- _Pixel2: area in pixel units

Features:
- Object_Label: Cell's label index, as in the corresponding label mask image.	
- Centroid_Time_Frames: Time point in the TIFF stack.
- Area_Pixel2: Cell's area.	
- BrightestPointToCentroidDistance_Pixel:
- BrightestPoint_X_Pixel, BrightestPoint_Y_Pixel: Coordinates of the brigthest point within the cell.
- Centroid_X_Pixel, Centroid_Y_Pixel, Centroid_Z_Pixel: Coordinates of the cell centroid.	
- ConvexArea_Pixel2: Area of the cell's convex hull.
- EllipsoidLongestAxisRadius_Pixel,	EllipsoidShortestAxisRadius_Pixel: Length of the longest and shortest axis of an elliposid that is fit to the cell pixels. 
- GeodesicDiameter_Pixel: The cell's geodesic diameter [REF]
- ImageBoundaryContact_Pixel: The number of pixels of the cell that are at the image boundary. This is useful to reject cells from the statistical analysis that are not fully in the image and therefore have invalid shape and intensity measurements.
- LargestInscribedCircleRadius_Pixel: The radius of the largest disk that can be enclosed within the corresponding cell.
- Perimeter_Pixel: The length of the cell's perimeter.
- RadiusAtBrightestPoint_Pixel
- SkeletonAvgBranchLength_Pixel:
- SkeletonLongestBranchLength_Pixel:
- SkeletonNumBranchPoints:
- SkeletonTotalLength_Pixel:

Other columns:
- FrameInterval, FrameInterval_Unit: Temporal calibration of the image.
- VoxelSpacing_Unit, VoxelSpacing_X, VoxelSpacing_Y, VoxelSpacing_Z: Spatial calibration of the image.
- Path_LabelMasks, Path_Intensities, Path_Skeletons, Path_Annotations: Relative paths to all associated images. This is very useful for downstream analysis and visualisation. 	


