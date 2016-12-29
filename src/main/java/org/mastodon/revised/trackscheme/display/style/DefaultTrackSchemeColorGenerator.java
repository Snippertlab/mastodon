package org.mastodon.revised.trackscheme.display.style;

import java.awt.Color;

import org.mastodon.revised.trackscheme.TrackSchemeEdge;
import org.mastodon.revised.trackscheme.TrackSchemeVertex;
import org.mastodon.revised.ui.EdgeColorGenerator;
import org.mastodon.revised.ui.VertexColorGenerator;

public class DefaultTrackSchemeColorGenerator implements VertexColorGenerator< TrackSchemeVertex >, EdgeColorGenerator< TrackSchemeEdge >
{

	private final Color vertexColor;

	private final Color edgeColor;

	public DefaultTrackSchemeColorGenerator()
	{
		vertexColor = TrackSchemeStyle.defaultStyle().vertexFillColor;
		edgeColor = TrackSchemeStyle.defaultStyle().edgeColor;
	}

	@Override
	public final Color color( final TrackSchemeEdge edge )
	{
		return vertexColor;
	}

	@Override
	public final Color color( final TrackSchemeVertex vertex )
	{
		return edgeColor;
	}

}
