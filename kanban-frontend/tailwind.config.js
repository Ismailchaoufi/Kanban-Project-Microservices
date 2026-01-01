/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          500: '#1976d2',
          600: '#1565c0',
          700: '#0d47a1',
        }
      }
    },
  },
  plugins: [],
  corePlugins: {
    preflight: false, // Important pour Angular Material
  }
}
