/** Tailwind powers the shadcn-style primitives layered on top of Angular Material's a11y/CDK foundation. */
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {
      colors: {
        "trading-bg": "#0b0f14",
        "trading-panel": "#121822",
        "trading-accent": "#2dd4bf",
        "trading-buy": "#22c55e",
        "trading-sell": "#ef4444"
      }
    }
  },
  plugins: []
};
