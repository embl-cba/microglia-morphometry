package de.embl.cba.microglia.morphometry;

/*
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

//package net.imglib2.realtransform;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.realtransform.AbstractTranslation;
import net.imglib2.realtransform.TranslationGet;

/**
 * 1-dimensional translation.
 *
 * @author Stephan Saalfeld, Christian Tischer
 */

// TODO: pull request!

public class Translation1D extends AbstractTranslation implements Concatenable< TranslationGet >, PreConcatenable< TranslationGet >
{
    final static protected RealPoint[] constDs = new RealPoint[ 1 ];
    {
        constDs[ 0 ] = new RealPoint( 1.0 );
    }

    final protected Translation1D inverse;

    protected Translation1D( final double[] t, final Translation1D inverse )
    {
        super( t, constDs );

        assert t.length == numDimensions(): "Input dimensions do not match or are not 1.";

        this.inverse = inverse;
    }

    public Translation1D()
    {
        super( new double[ 1 ], constDs );
        inverse = new Translation1D( new double[ 1 ], this );
    }

    public Translation1D( final double tx )
    {
        this();
        set( tx );
    }

    public Translation1D( final double... t )
    {
        this();
        set( t );
    }

    public void set( final double tx )
    {
        t[ 0 ] = tx;

        inverse.t[ 0 ] = -tx;
    }

    /**
     * Set the translation vector.
     *
     * @param t
     *            t.length &lt;= the number of dimensions of this
     *            {@link net.imglib2.realtransform.Translation2D}
     */
    @Override
    public void set( final double... t )
    {
        assert t.length <= 1 : "Too many inputs.";

        try
        {
            this.t[ 0 ] = t[ 0 ];

            inverse.t[ 0 ] = -t[ 0 ];
        }
        catch ( final ArrayIndexOutOfBoundsException e )
        {}
    }

    @Override
    public void set( final double t, final int d )
    {
        assert d >= 0 && d < numDimensions(): "Dimensions index out of bounds.";

        this.t[ d ] = t;
        inverse.t[ d ] = -t;
    }

    @Override
    public void apply( final double[] source, final double[] target )
    {
        assert source.length >= numDimensions() && target.length >= numDimensions(): "Input dimensions too small.";

        target[ 0 ] = source[ 0 ] + t[ 0 ];
    }

    @Override
    public void apply( final float[] source, final float[] target )
    {
        assert source.length >= numDimensions() && target.length >= numDimensions(): "Input dimensions too small.";

        target[ 0 ] = ( float ) ( source[ 0 ] + t[ 0 ] );
    }

    @Override
    public void apply( final RealLocalizable source, final RealPositionable target )
    {
        assert source.numDimensions() >= numDimensions() && target.numDimensions() >= numDimensions(): "Input dimensions too small.";

        target.setPosition( source.getDoublePosition( 0 ) + t[ 0 ], 0 );
    }

    @Override
    public void applyInverse( final double[] source, final double[] target )
    {
        assert source.length >= numDimensions() && target.length >= numDimensions(): "Input dimensions too small.";

        source[ 0 ] = target[ 0 ] - t[ 0 ];;
    }

    @Override
    public void applyInverse( final float[] source, final float[] target )
    {
        assert source.length >= numDimensions() && target.length >= numDimensions(): "Input dimensions too small.";

        source[ 0 ] = ( float ) ( target[ 0 ] - t[ 0 ] );
    }

    @Override
    public void applyInverse( final RealPositionable source, final RealLocalizable target )
    {
        assert source.numDimensions() >= numDimensions() && target.numDimensions() >= numDimensions(): "Input dimensions too small.";

        source.setPosition( target.getDoublePosition( 0 ) - t[ 0 ], 0 );
    }

    @Override
    public Translation1D copy()
    {
        return new Translation1D( t );
    }

    @Override
    public double[] getRowPackedCopy()
    {
        final double[] matrix = new double[ 2 ];

        matrix[ 0 ] = 1;

        matrix[ 1 ] = t[ 0 ];

        return matrix;
    }

    @Override
    public Translation1D inverse()
    {
        return inverse;
    }

    @Override
    public Translation1D preConcatenate( final TranslationGet a )
    {
        set( t[ 0 ] + a.getTranslation( 0 ) );

        return this;
    }

    @Override
    public Class< TranslationGet > getPreConcatenableClass()
    {
        return TranslationGet.class;
    }

    @Override
    public Translation1D concatenate( final TranslationGet a )
    {
        return preConcatenate( a );
    }

    @Override
    public Class< TranslationGet > getConcatenableClass()
    {
        return TranslationGet.class;
    }

    @Override
    public String toString()
    {
        return "1d-translation: (" + t[ 0 ] + ")";
    }
}
