# Microglia Morphometry

A Fiji plugin for semi-automated segmentation, tracking and morphometric analysis of microglia cells in 2D images.

## Install

- Fiji
- Updates sites:
    - IJPB-Plugins 
    - ...

## Run

## Methods

The microglia segmentation and tracking is preformed semi-automated.

### Parameters

- `minimal_microglia_size` = 200 micrometer^2


### Automated conversion to binary masks

For all time points, the images are smoothed using an anisotropic diffusion filter [TODO: REF] and then individually binarised by means of an intensity threshold that is determined automatically from the respective image intensity histogram: The intensity at the histogram mode (`intensity_mode`) is computed as well as the right hand side (towards higher intensities) intensity where the count decreases to half the count at the mode (`intensity_rightHandHalfMode`). The threshold is then computed as `intensity_mode + 1.5 *( intensity_rightHandHalfBin - intensity_mode )`. On the resulting binary image connected components smaller than `minimal_microglia_size` are removed.

### Semi-automated instance segmentation and tracking

The instance segmentation (object detection) and tracking are linked and preformed with human interaction. The main challenge here is that microliga can be touching making it difficult to separate them fully automatically. 

In brieg, for each time point, the binary mask is first subjected to an automated object splitting algorithm. Next, there is the opportunity to manually paint 

#### Automated object splitting

The binary masks are converted to label masks using connected component analysis. Each connected component is skeletonized and the total skeleton length (`skel_length`) is measured. A probable number of microglia within each connected component is computed as `n = ceil( skel_length / skel_maxLength)`, where `skel_maxLength` is set to 450 micrometer. For each connected component with `n > 1` the position of local intensity maxima (corresponding to likely microglia soma) is computed. The `n` brightest local intensity maxima are used as seed points to split the connected component using an intensity based watershed algorithm. Splits are only accepted if the resulting objects are larger than `minimal_microglia_size`.

###






