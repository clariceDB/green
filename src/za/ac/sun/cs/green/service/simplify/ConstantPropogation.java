package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropogation extends BasicService {


		/**
		 * Number of times the slicer has been invoked.
		 */
		private int invocations = 0;

		public ConstantPropogation(Green solver) {
			super(solver);
		}

		@Override
		public Set<Instance> processRequest(Instance instance) {
			@SuppressWarnings("unchecked")
			Set<Instance> result = (Set<Instance>) instance.getData(getClass());
			if (result == null) {
				final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
				final Expression e = simplify(instance.getFullExpression(), map);
				final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
				result = Collections.singleton(i);
				instance.setData(getClass(), result);
			}
			return result;
		}

		@Override
		public void report(Reporter reporter) {
			reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
		}

		public Expression simplify(Expression expression,
				Map<Variable, Variable> map) {
			try {
				log.log(Level.FINEST, "Before Canonization: " + expression);
				invocations++;
				PropogatingVisitor propVisitor = new PropogatingVisitor();
				expression.accept(propVisitor);
				expression = propVisitor.getExpression();
				//CanonizationVisitor canonizationVisitor = new CanonizationVisitor();
			//	expression.accept(canonizationVisitor);
				//Expression canonized = canonizationVisitor.getExpression();
				// if (canonized != null) {
				// 	canonized = new Renamer(map,
				// 			canonizationVisitor.getVariableSet()).rename(canonized);
				// }
				log.log(Level.FINEST, "After ConstProps: " + expression);
				return expression;
				// return canonized;
			} catch (VisitorException x) {
				log.log(Level.SEVERE,
						"encountered an exception -- this should not be happening!",
						x);
			}
			return null;
		}

		private static class PropogatingVisitor extends Visitor {
			private Stack<Expression> stack;
			private HashMap<IntVariable, IntConstant> hMap;

			public PropogatingVisitor() {
				stack = new Stack<Expression>();
				hMap = new HashMap<IntVariable,IntConstant>();
			}
			public Expression getExpression() {
				Expression popExpr = stack.pop();
				System.out.println("Expression popped: " + popExpr);
				return popExpr;
			}
			@Override
			public void postVisit(IntConstant constant) {
				System.out.println("Post visit constant: " + constant);
				stack.push(constant);
			}

			@Override
			public void postVisit(IntVariable variable) {
				System.out.println("Post visit variable: " + variable);
				stack.push(variable);
			}
			@Override
			public void postVisit(Operation operation) throws VisitorException {
				System.out.println("Post vist operation: " + operation);
				Operation.Operator op = operation.getOperator();
				if (op.equals(Operation.Operator.EQ)) {
					Expression rightE = stack.pop();
					Expression leftE = stack.pop();
					if ((rightE instanceof IntConstant) && (leftE instanceof IntVariable)) {
						hMap.put((IntVariable)leftE, (IntConstant)rightE);

					}  else if ((rightE instanceof IntVariable) && (leftE instanceof IntConstant)) {
						hMap.put((IntVariable)rightE, (IntConstant)leftE);
					}
					operation = new Operation(op, leftE, rightE);
				} else {
					Expression rightE = stack.pop();
					Expression leftE = stack.pop();
					if (rightE instanceof IntVariable && hMap.containsKey(rightE)) {
						rightE = hMap.get(rightE);
					}
					if (leftE instanceof IntVariable && hMap.containsKey(leftE)) {
						leftE = hMap.get(leftE);
					}
					operation = new Operation(op, leftE, rightE);
				}
				stack.push(operation);

			}
		}
}
