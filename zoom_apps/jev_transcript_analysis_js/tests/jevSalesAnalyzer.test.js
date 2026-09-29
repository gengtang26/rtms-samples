import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeJevResponse, SALES_QUESTIONS } from '../jevSalesAnalyzer.js';

test('uses all three Jev decision primitives', () => {
  const types = new Set(Object.values(SALES_QUESTIONS).map((question) => question.type));
  assert.deepEqual(types, new Set(['choice', 'noul', 'score']));
});

test('normalizes a Jev response into UI-safe sales analysis', () => {
  const result = normalizeJevResponse({
    model: 'typesafe/jev-1.13-20260917',
    answers: {
      buyer_intent: { type: 'choice', choice: 'pricing', confidence: 0.91, probabilities: { pricing: 0.91 } },
      objection: { type: 'choice', choice: 'budget', confidence: 0.72 },
      deal_stage: { type: 'choice', choice: 'negotiation', confidence: 0.83 },
      next_action: {
        type: 'choice',
        choice: 'address_objection',
        confidence: 0.88,
        probabilities: {
          clarify_need: 0.07,
          address_objection: 0.88,
          pause_and_listen: 0.03,
          unknown_action: 0.02
        }
      },
      purchase_signal: { type: 'noul', noul: 0.67 },
      deal_risk: { type: 'score', score: 2.4, confidence: 0.76 }
    },
    usage: { input_tokens: 120, output_tokens: 0 }
  });

  assert.equal(result.buyerIntent.value, 'pricing');
  assert.equal(result.nextAction.value, 'address_objection');
  assert.equal(result.nextAction.label, 'Address the objection');
  assert.match(result.nextAction.suggestion, /concern/i);
  assert.deepEqual(result.actionOptions.map((option) => option.value), [
    'address_objection',
    'clarify_need',
    'pause_and_listen'
  ]);
  assert.equal(result.actionOptions[0].recommended, true);
  assert.equal(result.purchaseSignal, 0.67);
  assert.equal(result.dealRisk.value, 2.4);
});

test('rejects a response without typed answers', () => {
  assert.throws(() => normalizeJevResponse({}), /answers object/);
});
