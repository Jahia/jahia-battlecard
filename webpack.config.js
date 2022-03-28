const path = require('path');
const webpack = require('webpack');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const moduleName = 'jahia-battlecard';

module.exports = (env, argv) => {
    // Get manifest
    let manifest = '';
    const files = require('fs').readdirSync(path.join(__dirname, './target/dependency'));
    if (files.length > 0) {
        manifest = `./target/dependency/${files[0]}`;
        console.log(`Jahia Battlecard module uses manifest: ${manifest}`);
    }

    let config = {
        entry: {main: [path.resolve(__dirname, 'src/javascript/publicPath'), path.resolve(__dirname, 'src/javascript/index.js')]},
        output: {
            path: path.resolve(__dirname, 'src/main/resources/javascript/apps/'),
            filename: `${moduleName}.bundle.js`,
            chunkFilename: `[name].${moduleName}.[chunkhash:6].js`
        },
        resolve: {
            mainFields: ['module', 'main'],
            extensions: ['.mjs', '.js', '.jsx', 'json']
        },
        optimization: {
            splitChunks: {
                maxSize: 400000
            }
        },
        module: {
            rules: [
                {
                    test: /\.jsx?$/,
                    include: [path.join(__dirname, 'src')],
                    use: {
                        loader: 'babel-loader',
                        options: {
                            presets: [
                                ['@babel/preset-env', {modules: false, targets: {safari: '7', ie: '10'}}],
                                '@babel/preset-react'
                            ],
                            plugins: ['@babel/plugin-syntax-dynamic-import']
                        }
                    }
                },
                {
                    test: /\.s[ac]ss$/i,
                    use: [
                        'style-loader',
                        {
                            loader:'css-loader',
                            options: {
                                modules: true
                            }
                        },
                        'sass-loader'
                    ]
                }
            ]
        },
        plugins: [
            new webpack.DllReferencePlugin({manifest: require(manifest)}),
            new CleanWebpackPlugin({verbose: false}),
            new webpack.ids.HashedModuleIdsPlugin({
                hashFunction: 'sha256',
                hashDigest: 'hex',
                hashDigestLength: 20
            }),
            new CopyWebpackPlugin({patterns: [{from: './package.json', to: ''}]})
        ],
        mode: argv.mode,
    };

    config.devtool = (argv.mode === 'production') ? 'source-map' : 'eval-source-map';

    if (argv.analyze) {
        config.devtool = 'source-map';
        config.plugins.push(new BundleAnalyzerPlugin());
    }

    return config;
};
