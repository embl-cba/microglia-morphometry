package de.embl.cba.microglia.track;

import de.embl.cba.microglia.Utils;
import de.embl.cba.microglia.morphometry.SyncWindowsHack;
import de.embl.cba.microglia.morphometry.regions.Regions;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import static de.embl.cba.microglia.Utils.saveLabels;

public class TrackingSplitterManualCorrectionUI< T extends RealType< T > & NativeType< T > >
		extends JPanel
{
	private final long minimalObjectSizeInPixels;
	private final Calibration calibration;
	private JFrame frame;
	private boolean isThisFrameFinished;
	private ImagePlus editableLabelsImp;
	private ArrayList< RandomAccessibleInterval< T > > labels;
	private SyncWindowsHack syncWindows;
	private static Point frameLocation;
	private static Point editedLabelsImpLocation;
	private boolean isStopped;
	private final String outputLabelingsPath;

	public TrackingSplitterManualCorrectionUI(
			ArrayList< RandomAccessibleInterval< T > > labels,
			long minimalObjectSizeInPixels,
			String outputLabelingsPath,
			Calibration calibration )
	{
		this.outputLabelingsPath = outputLabelingsPath;
		this.isThisFrameFinished = false;
		this.minimalObjectSizeInPixels = minimalObjectSizeInPixels;
		this.calibration = calibration;

		showLabelsForEditing( labels );

		configureAndShowUI();
	}

	public void configureAndShowUI()
	{
		add( drawButton() );

		add( deleteButton() );

		add( updateLabelsButton() );

		add( nextFrameButton() );

		add( stopAndSaveButton() );

		add( saveButton() );

		add( helpButton() );

		showPanel();
	}

	public void showLabelsForEditing( ArrayList< RandomAccessibleInterval< T > > labels )
	{
		editableLabelsImp = Utils.labelingsAsImagePlus( labels );
		editableLabelsImp.show();
		if ( editedLabelsImpLocation != null )
			editableLabelsImp.getWindow().setLocation( editedLabelsImpLocation );
		editableLabelsImp.setT( editableLabelsImp.getNFrames() );
		editableLabelsImp.updateImage();
		editableLabelsImp.setActivated();
		IJ.run( editableLabelsImp, "Enhance Contrast", "saturated=0.00");
		IJ.run("Brightness/Contrast...");

		syncWindows = new SyncWindowsHack();
		syncWindows.syncAll();
	}

	public JButton updateLabelsButton()
	{
		final JButton button = new JButton( "Update labels" );
		button.addActionListener( e -> {
			labels = runMaximalOverlapTrackerOnEditedImagePlus();
			closeCurrentEditedLabelsImagePlus();
			showLabelsForEditing( labels );
		} );

		return button;
	}

	public void closeCurrentEditedLabelsImagePlus()
	{
		editableLabelsImp.changes = false;
		editedLabelsImpLocation = editableLabelsImp.getWindow().getLocation();
		editableLabelsImp.close();

		syncWindows.close();
	}

	public JButton deleteButton()
	{
		final JButton button = new JButton( "Erase" );
		button.addActionListener( e -> {
			Toolbar.setForegroundColor( new Color( 0, 0,0 ) );
			//editableLabelsImp.getProcessor().setValue( 0 );
		} );
		return button;
	}

	public JButton drawButton()
	{
		final JButton button = new JButton( "Draw" );
		button.addActionListener( e -> {
			Toolbar.setForegroundColor( new Color( 255, 255,0 ) );
		} );
		return button;
	}

	public JButton nextFrameButton()
	{
		final JButton button = new JButton( "Next frame" );
		button.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				labels = runMaximalOverlapTrackerOnEditedImagePlus();
				closeCurrentEditedLabelsImagePlus();
				frameLocation = frame.getLocation();
				frame.dispose();

				isThisFrameFinished = true;
				isStopped = false;
			}
		} );
		return button;
	}

	public JButton stopAndSaveButton()
	{
		final JButton button = new JButton( "Stop and Save" );
		button.addActionListener( e -> {
			labels = runMaximalOverlapTrackerOnEditedImagePlus();
			closeCurrentEditedLabelsImagePlus();
			frameLocation = frame.getLocation();
			frame.dispose();
			isThisFrameFinished = true;
			isStopped = true;
		} );
		return button;
	}

	public JButton saveButton()
	{
		final JButton button = new JButton( "Save" );
		button.addActionListener( e -> SwingUtilities.invokeLater( () -> {
			labels = runMaximalOverlapTrackerOnEditedImagePlus();
			saveLabels( labels, calibration, outputLabelingsPath );
		} ) );
		return button;
	}

	public JButton helpButton()
	{
		final JButton button = new JButton( "Help" );
		button.addActionListener( e -> SwingUtilities.invokeLater( () -> {
			Utils.openURI( "https://github.com/embl-cba/microglia-morphometry#manual-label-mask-correction" );
		} ) );
		return button;
	}

	private void showPanel() {

		//Create and set up the window.
		frame = new JFrame("Manual label editing");

		//Create and set up the content pane.
		this.setOpaque(true); //content panes must be opaque
		frame.setContentPane(this);

		//Display the window.
		frame.pack();
		if ( frameLocation != null ) frame.setLocation( frameLocation );
		frame.setVisible( true );

	}

	public boolean isThisFrameFinished()
	{
		return isThisFrameFinished;
	}

	public boolean isStopped()
	{
		return isStopped;
	}

	public ArrayList< RandomAccessibleInterval< T > > runMaximalOverlapTrackerOnEditedImagePlus()
	{
		final ArrayList< RandomAccessibleInterval< IntType > > labels = Utils.asIntType( Utils.get2DImagePlusMovieAsFrameList( editableLabelsImp, 1 ) );

		// Due to the editing small unconnected regions of pixels may occur
		Regions.removeSmallRegionsInMasks( labels, minimalObjectSizeInPixels );

		// TODO: if below turns out to be too slow, only do it for the last two frames
		final MaximalOverlapTracker maximalOverlapTracker = new MaximalOverlapTracker( labels );
		maximalOverlapTracker.run();

		return maximalOverlapTracker.getLabelings();
	}

	public ArrayList< RandomAccessibleInterval< T > > getLabelings()
	{
		return labels;
	}


}
