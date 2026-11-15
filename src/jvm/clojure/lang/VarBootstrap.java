/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

package clojure.lang;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;

public final class VarBootstrap {

private static final MethodHandle RELINK_MH;
private static final MethodHandle SLOW_CALL_MH;

static {
	try {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		RELINK_MH = lookup.findStatic(
			VarBootstrap.class,
			"relinkAndInvoke",
			MethodType.methodType(Object.class, SelfSite.class, Object[].class)
		);
		SLOW_CALL_MH = lookup.findStatic(
			VarBootstrap.class,
			"slowCall",
			MethodType.methodType(Object.class, SelfSite.class, Object[].class)
		);
	} catch (Throwable t) {
		throw Util.sneakyThrow(t);
	}
}

static final class SelfSite extends MutableCallSite {
	final MethodHandles.Lookup lookup;
	final String targetMethodName;
	final Class<?> fnClass;
	final Var var;
	final boolean variadic;
	final MethodHandle relinkHandle;
	MethodHandle fastHandle;
	MethodHandle slowHandle;

	SelfSite(MethodHandles.Lookup lookup,
	         String targetMethodName,
	         MethodType callType,
	         Class<?> fnClass,
	         Var var,
	         boolean variadic) {
		super(callType);
		this.lookup = lookup;
		this.targetMethodName = targetMethodName;
		this.fnClass = fnClass;
		this.var = var;
		this.variadic = variadic;
		this.relinkHandle = MethodHandles.insertArguments(RELINK_MH, 0, this)
			.asCollector(Object[].class, callType.parameterCount())
			.asType(callType);
		setTarget(this.relinkHandle);
	}

	MethodHandle getFastHandle() throws Throwable {
		MethodHandle fh = this.fastHandle;
		if (fh == null) {
			MethodType mt = type();
			fh = lookup.findStatic(fnClass, targetMethodName, mt);
			this.fastHandle = fh;
		}
		return fh;
	}

	MethodHandle getSlowHandle() {
		MethodHandle sh = this.slowHandle;
		if (sh == null) {
			MethodType mt = type();
			sh = MethodHandles.insertArguments(SLOW_CALL_MH, 0, this)
				.asCollector(Object[].class, mt.parameterCount())
				.asType(mt);
			this.slowHandle = sh;
		}
		return sh;
	}
}

public static CallSite bootstrapSelf(MethodHandles.Lookup lookup,
                                     String targetMethodName,
                                     MethodType callType,
                                     Class<?> fnClass,
                                     String nsName,
                                     String varName,
                                     int isVariadic) {
	Var v = RT.var(nsName, varName);
	return new SelfSite(lookup, targetMethodName, callType, fnClass, v, isVariadic != 0);
}

private static Object relinkAndInvoke(SelfSite site, Object[] args) throws Throwable {
	Var v = site.var;
	SwitchPoint sp = v.getSwitchPoint();
	if (!v.isDynamic()) {
		Object root = v.getRawRoot();
		if (root != null && root.getClass() == site.fnClass) {
			MethodHandle fast = site.getFastHandle();
			site.setTarget(sp.guardWithTest(fast, site.relinkHandle));
			return fast.invokeWithArguments(args);
		}
	}
	MethodHandle slow = site.getSlowHandle();
	site.setTarget(sp.guardWithTest(slow, site.relinkHandle));
	return slowCall(site, args);
}

private static Object slowCall(SelfSite site, Object[] args) throws Throwable {
	IFn fn = site.var.fn();
	Object res;
	if (site.variadic) {
		int reqCount = args.length - 1;
		ISeq seq = (ISeq) args[reqCount];
		for (int i = reqCount - 1; i >= 0; i--) {
			seq = RT.cons(args[i], seq);
		}
		res = fn.applyTo(seq);
	} else {
		switch (args.length) {
			case 0: res = fn.invoke(); break;
			case 1: res = fn.invoke(args[0]); break;
			case 2: res = fn.invoke(args[0], args[1]); break;
			case 3: res = fn.invoke(args[0], args[1], args[2]); break;
			case 4: res = fn.invoke(args[0], args[1], args[2], args[3]); break;
			case 5: res = fn.invoke(args[0], args[1], args[2], args[3], args[4]); break;
			case 6: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5]); break;
			case 7: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6]); break;
			case 8: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]); break;
			case 9: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]); break;
			case 10: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]); break;
			case 11: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10]); break;
			case 12: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11]); break;
			case 13: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12]); break;
			case 14: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13]); break;
			case 15: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14]); break;
			case 16: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15]); break;
			case 17: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16]); break;
			case 18: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17]); break;
			case 19: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18]); break;
			case 20: res = fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19]); break;
			default:
				res = fn.applyTo(ArraySeq.create(args));
				break;
		}
	}
	Class<?> retType = site.type().returnType();
	if (retType == long.class) {
		return RT.longCast(res);
	} else if (retType == double.class) {
		return RT.doubleCast(res);
	}
	return res;
}
}
