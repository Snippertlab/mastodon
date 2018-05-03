package org.mastodon.revised.mamut;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.separator;
import static org.mastodon.revised.mamut.MamutMenuBuilder.editMenu;
import static org.mastodon.revised.mamut.MamutMenuBuilder.fileMenu;
import static org.mastodon.revised.mamut.MamutMenuBuilder.viewMenu;

import javax.swing.ActionMap;

import org.jdom2.Element;
import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.model.AutoNavigateFocusModel;
import org.mastodon.revised.bdv.BdvContextProvider;
import org.mastodon.revised.bdv.BigDataViewerActionsMamut;
import org.mastodon.revised.bdv.BigDataViewerMamut;
import org.mastodon.revised.bdv.NavigationActionsMamut;
import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.bdv.ViewerFrameMamut;
import org.mastodon.revised.bdv.ViewerPanelMamut;
import org.mastodon.revised.bdv.overlay.BdvHighlightHandler;
import org.mastodon.revised.bdv.overlay.BdvSelectionBehaviours;
import org.mastodon.revised.bdv.overlay.EditBehaviours;
import org.mastodon.revised.bdv.overlay.EditSpecialBehaviours;
import org.mastodon.revised.bdv.overlay.OverlayGraphRenderer;
import org.mastodon.revised.bdv.overlay.OverlayNavigation;
import org.mastodon.revised.bdv.overlay.RenderSettings;
import org.mastodon.revised.bdv.overlay.RenderSettings.UpdateListener;
import org.mastodon.revised.bdv.overlay.wrap.OverlayEdgeWrapper;
import org.mastodon.revised.bdv.overlay.wrap.OverlayGraphWrapper;
import org.mastodon.revised.bdv.overlay.wrap.OverlayVertexWrapper;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.ModelOverlayProperties;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.revised.ui.FocusActions;
import org.mastodon.revised.ui.HighlightBehaviours;
import org.mastodon.revised.ui.SelectionActions;
import org.mastodon.views.context.ContextProvider;

import bdv.tools.InitializeViewerState;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.state.ViewerState;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

public class MamutViewBdv extends MamutView< OverlayGraphWrapper< Spot, Link >, OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > >
{
	public static final String BDV_TYPE_VALUE = "BigDataViewer";
	private static final String VIEWER_TRANSFORM_TAG = "ViewerTransform";
	private static final String TIMEPOINT_TAG = "CurrentTimepoint";
	private static final String GROUP_TAG = "CurrentGroup";
	private static final String SOURCE_TAG = "CurrentSource";
	private static final String INTERPOLATION_TAG = "Interpolation";
	private static final String DISPLAY_MODE_TAG = "DisplayMode";

	// TODO
	private static int bdvName = 1;

	private final SharedBigDataViewerData sharedBdvData;

	private final BdvContextProvider< Spot, Link > contextProvider;

	private final ViewerPanelMamut viewer;

	public MamutViewBdv( final MamutAppModel appModel )
	{
		super( appModel,
				new OverlayGraphWrapper<>(
						appModel.getModel().getGraph(),
						appModel.getModel().getGraphIdBimap(),
						appModel.getModel().getSpatioTemporalIndex(),
						appModel.getModel().getGraph().getLock(),
						new ModelOverlayProperties( appModel.getModel().getGraph(), appModel.getRadiusStats() ) ),
				new String[] { KeyConfigContexts.BIGDATAVIEWER } );

		sharedBdvData = appModel.getSharedBdvData();

		final String windowTitle = "BigDataViewer " + ( bdvName++ ); // TODO: use JY naming scheme
		final BigDataViewerMamut bdv = new BigDataViewerMamut( sharedBdvData, windowTitle, groupHandle );
		final ViewerFrameMamut frame = bdv.getViewerFrame();
		setFrame( frame );

		MastodonFrameViewActions.install( viewActions, this );
		BigDataViewerActionsMamut.install( viewActions, bdv );

		final ViewMenu menu = new ViewMenu( this );
		final ActionMap actionMap = frame.getKeybindings().getConcatenatedActionMap();

		MainWindow.addMenus( menu, actionMap );
		MamutMenuBuilder.build( menu, actionMap,
				fileMenu(
						separator(),
						item( BigDataViewerActionsMamut.LOAD_SETTINGS ),
						item( BigDataViewerActionsMamut.SAVE_SETTINGS )
				),
				viewMenu(
						item( MastodonFrameViewActions.TOGGLE_SETTINGS_PANEL )
				),
				editMenu(
						item( UndoActions.UNDO ),
						item( UndoActions.REDO ),
						separator(),
						item( SelectionActions.DELETE_SELECTION ),
						item( SelectionActions.SELECT_WHOLE_TRACK ),
						item( SelectionActions.SELECT_TRACK_DOWNWARD ),
						item( SelectionActions.SELECT_TRACK_UPWARD )
				),
				ViewMenuBuilder.menu( "Settings",
						item( BigDataViewerActionsMamut.BRIGHTNESS_SETTINGS ),
						item( BigDataViewerActionsMamut.VISIBILITY_AND_GROUPING )
				)
		);
		appModel.getPlugins().addMenus( menu );

		frame.setLocationByPlatform( true );
		frame.setVisible( true );

		viewer = bdv.getViewer();
		InitializeViewerState.initTransform( viewer );

		viewer.setTimepoint( timepointModel.getTimepoint() );
		final OverlayGraphRenderer< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > tracksOverlay = new OverlayGraphRenderer<>(
				viewGraph,
				highlightModel,
				focusModel,
				selectionModel );
		viewer.getDisplay().addOverlayRenderer( tracksOverlay );
		viewer.addRenderTransformListener( tracksOverlay );
		viewer.addTimePointListener( tracksOverlay );

		final Model model = appModel.getModel();
		final ModelGraph modelGraph = model.getGraph();

		highlightModel.listeners().add( () -> viewer.getDisplay().repaint() );
		focusModel.listeners().add( () -> viewer.getDisplay().repaint() );
		modelGraph.addGraphChangeListener( () -> viewer.getDisplay().repaint() );
		modelGraph.addVertexPositionListener( ( v ) -> viewer.getDisplay().repaint() );
		selectionModel.listeners().add( () -> viewer.getDisplay().repaint() );

		final OverlayNavigation< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > overlayNavigation = new OverlayNavigation<>( viewer, viewGraph );
		navigationHandler.listeners().add( overlayNavigation );

		final BdvHighlightHandler< ?, ? > highlightHandler = new BdvHighlightHandler<>( viewGraph, tracksOverlay, highlightModel );
		viewer.getDisplay().addHandler( highlightHandler );
		viewer.addRenderTransformListener( highlightHandler );

		contextProvider = new BdvContextProvider<>( windowTitle, viewGraph, tracksOverlay );
		viewer.addRenderTransformListener( contextProvider );

		final AutoNavigateFocusModel< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > navigateFocusModel = new AutoNavigateFocusModel<>( focusModel, navigationHandler );

		BdvSelectionBehaviours.install( viewBehaviours, viewGraph, tracksOverlay, selectionModel, focusModel, navigationHandler );
		EditBehaviours.install( viewBehaviours, viewGraph, tracksOverlay, selectionModel, focusModel, model );
		EditSpecialBehaviours.install( viewBehaviours, frame.getViewerPanel(), viewGraph, tracksOverlay, selectionModel, focusModel, model );
		HighlightBehaviours.install( viewBehaviours, viewGraph, viewGraph.getLock(), viewGraph, highlightModel, model );
		FocusActions.install( viewActions, viewGraph, viewGraph.getLock(), navigateFocusModel, selectionModel );

		NavigationActionsMamut.install( viewActions, viewer );
		viewer.getTransformEventHandler().install( viewBehaviours );

		viewer.addTimePointListener( timePointIndex -> timepointModel.setTimepoint( timePointIndex ) );
		timepointModel.listeners().add( () -> viewer.setTimepoint( timepointModel.getTimepoint() ) );

		final RenderSettings renderSettings = appModel.getRenderSettingsManager().getForwardDefaultStyle();
		tracksOverlay.setRenderSettings( renderSettings );
		final UpdateListener updateListener = () -> {
			viewer.repaint();
			contextProvider.notifyContextChanged();
		};
		renderSettings.updateListeners().add( updateListener );
		onClose( () -> renderSettings.updateListeners().remove( updateListener ) );

//		if ( !bdv.tryLoadSettings( bdvFile ) ) // TODO
//			InitializeViewerState.initBrightness( 0.001, 0.999, bdv.getViewer(), bdv.getSetupAssignments() );
	}

	public ContextProvider< Spot > getContextProvider()
	{
		return contextProvider;
	}

	public void requestRepaint()
	{
		viewer.requestRepaint();
	}

	@Override
	public Element toXml()
	{
		final Element element = super.toXml();
		element.setAttribute( VIEW_TYPE_TAG, BDV_TYPE_VALUE );

		final ViewerState state = viewer.getState();
		element.addContent( XmlHelpers.intElement( TIMEPOINT_TAG, state.getCurrentTimepoint() ) );
		element.addContent( XmlHelpers.intElement( GROUP_TAG, state.getCurrentGroup() ) );
		element.addContent( XmlHelpers.intElement( SOURCE_TAG, state.getCurrentSource() ) );
		element.addContent( XmlHelpers.textElement( INTERPOLATION_TAG, state.getInterpolation().name() ) );
		element.addContent( XmlHelpers.textElement( DISPLAY_MODE_TAG, state.getDisplayMode().name() ) );
		final AffineTransform3D t = new AffineTransform3D();
		state.getViewerTransform( t );
		element.addContent( XmlHelpers.affineTransform3DElement( VIEWER_TRANSFORM_TAG, t ) );

		return element;
	}

	@Override
	public void restoreFromXml( final Element element )
	{
		super.restoreFromXml( element );
		try
		{
			final int numTimepoints = sharedBdvData.getNumTimepoints();
			int timepoint = XmlHelpers.getInt( element, TIMEPOINT_TAG );
			timepoint = Math.max( timepoint, 0 );
			timepoint = Math.min( timepoint, numTimepoints - 1 );
			viewer.setTimepoint( timepoint );
		}
		catch (final NumberFormatException nfe)
		{}
		final VisibilityAndGrouping visibilityAndGrouping = viewer.getVisibilityAndGrouping();
		try
		{
			final int group = XmlHelpers.getInt( element, GROUP_TAG );
			visibilityAndGrouping.setCurrentGroup( group );
		}
		catch (final NumberFormatException nfe)
		{}
		try
		{
			final int source = XmlHelpers.getInt( element, SOURCE_TAG );
			visibilityAndGrouping.setCurrentSource( source );
		}
		catch (final NumberFormatException nfe)
		{}
		try
		{
			final Interpolation interpolation = Interpolation.valueOf( XmlHelpers.getText( element, INTERPOLATION_TAG, Interpolation.NLINEAR.name() ) );
			viewer.setInterpolation( interpolation );
		}
		catch ( final IllegalArgumentException iae )
		{}
		try
		{
			final DisplayMode displayMode = DisplayMode.valueOf( XmlHelpers.getText( element, DISPLAY_MODE_TAG, DisplayMode.SINGLE.name() ) );
			viewer.setDisplayMode( displayMode );
		}
		catch ( final IllegalArgumentException iae )
		{}
		try
		{
			final AffineTransform3D t = XmlHelpers.getAffineTransform3D( element, VIEWER_TRANSFORM_TAG );
			if ( null != t )
			{
				new Thread( () -> {
					try
					{
						Thread.sleep( 100 );
						viewer.setCurrentViewerTransform( t );
					}
					catch ( final InterruptedException e )
					{
						e.printStackTrace();
					}
				} ).start();
			}
		}
		catch ( final NumberFormatException nfe )
		{}
	}
}
