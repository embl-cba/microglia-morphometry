/*-
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
package de.embl.cba.microglia.table;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

public class ColumnClassAwareTableModel extends DefaultTableModel
{
	List< Class > columnClasses;

	public ColumnClassAwareTableModel( )
	{
		super();
	}

	@Override
	public Class getColumnClass( int column )
	{
		return columnClasses.get( column );
	}

	@Override
	public boolean isCellEditable( int row, int column )
	{
		return true;
	}

	/**
	 * Determines getColumnName classes from entries in 1st row.
	 */
	public void refreshColumnClassesFromObjectColumns()
	{
		columnClasses = new ArrayList<>(  );

		for ( int column = 0; column < getColumnCount(); column++ )
		{
			try
			{
				final Object value = this.getValueAt( 0, column );
				columnClasses.add( value.getClass() );
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				final String columnName = this.getColumnName( column );
				throw new RuntimeException( "Got null at row 0 for column: " + columnName );
			}
		}
	}

	public void addColumnClass( Object aValue )
	{
		columnClasses.add( aValue.getClass() );
	}

}
