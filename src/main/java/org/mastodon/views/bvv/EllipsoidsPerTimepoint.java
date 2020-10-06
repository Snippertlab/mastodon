package org.mastodon.views.bvv;

public interface EllipsoidsPerTimepoint< V extends BvvVertex< V, E >, E extends BvvEdge< E, V > >
{
	EllipsoidInstances< V, E > forTimepoint( final int timepoint );
}
