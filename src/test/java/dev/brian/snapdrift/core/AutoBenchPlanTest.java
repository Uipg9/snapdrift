package dev.brian.snapdrift.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class AutoBenchPlanTest {
	@Test
	void expandsRepeatsInOrder() {
		List<AutoBenchPlan.Step> steps = AutoBenchPlan.parse("warmup20@20, rd20@20x3");
		assertEquals(List.of(
			new AutoBenchPlan.Step("warmup20", 20),
			new AutoBenchPlan.Step("rd20", 20),
			new AutoBenchPlan.Step("rd20", 20),
			new AutoBenchPlan.Step("rd20", 20)), steps);
	}

	@Test
	void renderDistanceIsOptional() {
		assertEquals(List.of(new AutoBenchPlan.Step("run", -1), new AutoBenchPlan.Step("run", -1)),
			AutoBenchPlan.parse("runx2"));
	}

	@Test
	void blankPlanIsEmpty() {
		assertEquals(List.of(), AutoBenchPlan.parse(""));
		assertEquals(List.of(), AutoBenchPlan.parse(null));
	}

	@Test
	void rejectsBadSteps() {
		assertThrows(IllegalArgumentException.class, () -> AutoBenchPlan.parse("rd@abc"));
		assertThrows(IllegalArgumentException.class, () -> AutoBenchPlan.parse("rd@1"));   // below vanilla minimum 2
		assertThrows(IllegalArgumentException.class, () -> AutoBenchPlan.parse("rd@40")); // above vanilla maximum 32
		assertThrows(IllegalArgumentException.class, () -> AutoBenchPlan.parse("@12"));
		assertThrows(IllegalArgumentException.class, () -> AutoBenchPlan.parse("rd@12x0"));
	}
}
