package org.mastodon.views.bvv.pool.attributes;

import org.joml.Vector3fc;

public interface Vector3fAttributeValue extends Vector3fAttributeReadOnlyValue
{
	void set( final Vector3fc value );

	void set( final float x, final float y, final float z );

	void set( final Vector3fAttributeReadOnlyValue value );

	void zero();
}
