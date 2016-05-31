package net.trackmate.graph;

import net.trackmate.graph.FeatureRegistry.DuplicateKeyException;
import net.trackmate.graph.features.unify.FeatureCleanup;
import net.trackmate.graph.features.unify.NotifyFeatureValueChange;
import net.trackmate.graph.features.unify.UndoFeatureMap;

/**
 * Mother class for edge features. TODO
 *
 * @param <M>
 * @param <E>
 * @param <F>
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Jean-Yves Tinevez&lt;jeanyves.tinevez@gmail.com&gt;
 */
public abstract class EdgeFeature< M, E extends Edge< ? >, F extends FeatureValue< ? > >
{
	private final String key;

	/**
	 * Unique ID. These IDs are generated by FeatureRegistry, starting from 0.
	 * As long as there are not excessively many VertexFeatures, the ID can be
	 * used as an index to look up features in a list instead of a map.
	 */
	private final int id;

	protected EdgeFeature( final String key ) throws DuplicateKeyException
	{
		this.key = key;
		this.id = FeatureRegistry.getUniqueEdgeFeatureId( key );
		FeatureRegistry.registerEdgeFeature( this );
	}

	public String getKey()
	{
		return key;
	}

	/*
	 * Following part is for the graph to create feature maps, initialize
	 * features, serialize, etc...
	 */

	protected abstract M createFeatureMap( final ReadOnlyGraph< ?, E > graph );

	public abstract F createFeatureValue( E edge, GraphFeatures< ?, E > graphFeatures );

	protected abstract FeatureCleanup< E > createFeatureCleanup( M featureMap );

	public abstract UndoFeatureMap< E > createUndoFeatureMap( M featureMap );

	public int getUniqueFeatureId()
	{
		return id;
	}

	protected static class NotifyValueChange< E extends Edge< ? > > implements NotifyFeatureValueChange
	{
		private final GraphFeatures< ?, E > graphFeatures;

		private final EdgeFeature< ?, E, ? > feature;

		private final E edge;

		public NotifyValueChange( final GraphFeatures< ?, E > graphFeatures, final EdgeFeature< ?, E, ? > feature, final E edge )
		{
			this.graphFeatures = graphFeatures;
			this.feature = feature;
			this.edge = edge;
		}

		@Override
		public void notifyBeforeFeatureChange()
		{
			graphFeatures.notifyBeforeFeatureChange( feature, edge );
		}
	}

	@Override
	public int hashCode()
	{
		return id;
	}

	@Override
	public boolean equals( final Object obj )
	{
		return obj instanceof EdgeFeature
				&& ( ( EdgeFeature< ?, ?, ? > ) obj ).key.equals( key );
	}

	@Override
	public String toString()
	{
		return getClass().getName() + "(\"" + key + "\")";
	}
}
