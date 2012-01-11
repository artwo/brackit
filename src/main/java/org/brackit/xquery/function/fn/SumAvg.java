/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.function.fn;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.DTD;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.YMD;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class SumAvg extends AbstractFunction {
	private final boolean avg;

	public SumAvg(QNm name, Signature signature, boolean avg) {
		super(name, signature, true);
		this.avg = avg;
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx,
			Sequence[] args) throws QueryException {
		Sequence seq = args[0];
		Item item;
		Atomic agg = null;
		Type aggType = null;

		if (seq == null) {
			if (avg) {
				return null;
			}
			return (args.length == 2) ? args[1] : Int32.ZERO;
		}

		Iter in = seq.iterate();
		try {
			if ((item = in.next()) != null) {
				agg = item.atomize();
				aggType = agg.type();

				if (aggType == Type.UNA) {
					agg = Cast.cast(null, agg, Type.DBL, false);
					aggType = Type.DBL;
				}

				if (aggType.isNumeric()) {
					agg = numericAggregate(ctx, in, (Numeric) agg);
				} else if (aggType.instanceOf(Type.YMD)) {
					agg = ymdAggregate(ctx, in, (YMD) agg);
				} else if (aggType.instanceOf(Type.DTD)) {
					agg = dtdAggregate(ctx, in, (DTD) agg);
				} else {
					throw new QueryException(
							ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
							"Cannot compute sum/avg for items of type: %s",
							aggType);
				}
			}
		} finally {
			in.close();
		}

		return ((agg != null) || (avg)) ? agg : ((args.length == 2) ? args[1]
				: Int32.ZERO);
	}

	private Atomic numericAggregate(QueryContext ctx, Iter in, Numeric agg)
			throws QueryException {
		Item item;
		IntNumeric count = Int32.ONE;

		while ((item = in.next()) != null) {
			Atomic s = item.atomize();
			Type type = s.type();

			if (type == Type.UNA) {
				s = Cast.cast(null, s, Type.DBL, false);
				type = Type.DBL;
			} else if (!(s instanceof Numeric)) {
				throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
						"Incompatible types in aggregate function: %s and %s.",
						agg, type);
			}

			agg = agg.add((Numeric) s);
			count = count.inc();
		}
		return (Atomic) (avg ? agg.div(count) : agg);
	}

	private Atomic ymdAggregate(QueryContext ctx, Iter in, YMD agg)
			throws QueryException {
		Item item;
		IntNumeric count = Int32.ONE;

		while ((item = in.next()) != null) {
			Atomic s = item.atomize();
			Type type = s.type();

			if (type == Type.UNA) {
				s = Cast.cast(null, s, Type.YMD, false);
			} else if (!type.instanceOf(Type.YMD)) {
				throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
						"Incompatible types in aggregate function: %s and %s.",
						Type.YMD, type);
			}

			agg = agg.add((YMD) s);
			count = count.inc();
		}
		return (Atomic) (avg ? agg.divide(new Dbl(count.doubleValue())) : agg);
	}

	private Atomic dtdAggregate(QueryContext ctx, Iter in, DTD agg)
			throws QueryException {
		Item item;
		IntNumeric count = Int32.ONE;

		while ((item = in.next()) != null) {
			Atomic s = item.atomize();
			Type type = s.type();

			if (type == Type.UNA) {
				s = Cast.cast(null, s, Type.DTD, false);
			} else if (!type.instanceOf(Type.DTD)) {
				throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
						"Incompatible types in aggregate function: %s and %s.",
						Type.DTD, type);
			}

			agg = agg.add((DTD) s);
			count = count.inc();
		}
		return (Atomic) (avg ? agg.divide(new Dbl(count.doubleValue())) : agg);
	}
}