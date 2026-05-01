import { validateMemoQuizCardsJson } from './memo-quiz-json-import-dialog';

describe('validateMemoQuizCardsJson', () => {
  it('maps valid question and answer JSON to API front and back fields', () => {
    const result = validateMemoQuizCardsJson(
      JSON.stringify([{ question: ' Question ', answer: ' Answer ' }]),
    );

    expect(result.detectedCount).toBe(1);
    expect(result.errors).toEqual([]);
    expect(result.cards).toEqual([{ front: 'Question', back: 'Answer' }]);
  });

  it('rejects invalid JSON roots and empty arrays', () => {
    expect(validateMemoQuizCardsJson('{').errors).toContain('Le JSON est invalide.');
    expect(validateMemoQuizCardsJson('{}').errors).toContain('La racine JSON doit être un tableau.');
    expect(validateMemoQuizCardsJson('[]').errors).toContain('Le tableau ne doit pas être vide.');
  });

  it('rejects more than 100 cards', () => {
    const cards = Array.from({ length: 101 }, () => ({ question: 'Q', answer: 'A' }));

    const result = validateMemoQuizCardsJson(JSON.stringify(cards));

    expect(result.detectedCount).toBe(101);
    expect(result.errors).toContain('Le tableau ne doit pas contenir plus de 100 cartes.');
    expect(result.cards).toEqual([]);
  });

  it('reports item-level validation errors without returning importable cards', () => {
    const result = validateMemoQuizCardsJson(
      JSON.stringify([
        { question: 'Q', answer: 'A' },
        { question: ' ', answer: 'A' },
        { question: 'Q', answer: 'A', extra: true },
      ]),
    );

    expect(result.detectedCount).toBe(3);
    expect(result.errors).toEqual([
      'Élément 2 : question doit être une chaîne non vide.',
      'Élément 3 : doit contenir exactement question et answer.',
    ]);
    expect(result.cards).toEqual([]);
  });
});
