/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import coil.request.ImageRequest
import com.example.androiddevchallenge.ui.theme.MyTheme
import dev.chrisbanes.accompanist.coil.CoilImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.sargunvohra.lib.pokekotlin.client.PokeApi
import me.sargunvohra.lib.pokekotlin.client.PokeApiClient
import me.sargunvohra.lib.pokekotlin.model.Pokemon
import java.util.*
import kotlin.random.Random

data class PokemonEntity(
    val id: Int,
    val name: String,
    val imageUrl: String? = null,
    val primaryType: String = "none"
)

class PokemonListViewModel : ViewModel() {
    val list: StateFlow<List<PokemonEntity>> = MutableStateFlow(emptyList())
    private val pokeApi: PokeApi = PokeApiClient()

    fun fetchPokemon() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = pokeApi.getPokemonList(0, 20)
            (list as MutableStateFlow).value = result.results.map {
                pokeApi.getPokemon(it.id).let { pokemon ->
                    PokemonEntity(
                        pokemon.id,
                        pokemon.name,
                        pokemon.sprites.frontDefault,
                        pokemon.types.first().type.name
                    )
                }
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pokemonListViewModel = PokemonListViewModel()
        setContent {
            MyTheme {
                MyApp(pokemonListViewModel)
            }
        }
        pokemonListViewModel.fetchPokemon()
    }
}

// Start building your app here!
@Composable
fun MyApp(pokemonListViewModel: PokemonListViewModel) {
    val list = pokemonListViewModel.list.collectAsState().value
    val navController = rememberNavController()
    Surface(color = MaterialTheme.colors.background) {
        NavHost(navController = navController, startDestination = "list") {
            composable("list") { PokemonList(list, navController) }
            composable("detail/{pokemonId}", arguments = listOf(navArgument("pokemonId") {
                type = NavType.IntType
            })) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("pokemonId")
                PokemonDetail(list.find { it.id == id }!!, navController)
            }
            composable("thanks") { AdoptThanks() }
        }
    }
}

@ColorRes
fun PokemonEntity.getTypeResColor(): Int = when (primaryType.toLowerCase(Locale.getDefault())) {
    "grass", "bug" -> R.color.pokeLightTeal
    "fire" -> R.color.pokeLightRed
    "water", "fighting", "normal" -> R.color.pokeLightBlue
    "electric", "psychic" -> R.color.pokeLightYellow
    "poison", "ghost" -> R.color.pokeLightPurple
    "ground", "rock" -> R.color.pokeLightBrown
    "dark" -> R.color.pokeBlack
    else -> R.color.pokeLightBlue
}

@Composable
fun AdoptThanks() {
    Column(verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Text("You're now a Pokemon Trainer !")
    }
}

@Composable
fun PokemonDetail(pokemon: PokemonEntity, navController: NavController) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Text(pokemon.name.capitalize(Locale.getDefault()))
        pokemon.imageUrl?.let {
            CoilImage(
                data = it,
                contentDescription = pokemon.name,
                fadeIn = true,
                modifier = Modifier.height(128.dp)
            )
        }
        Button(onClick = { navController.navigate("thanks") {
            popUpTo("list") { }
        } }) {
            Text("Adapot")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PokemonList(pokemons: List<PokemonEntity>, navController: NavController) {
    LazyVerticalGrid(cells = GridCells.Fixed(2)) {
        items(pokemons) { pokemon ->
            Card(
                backgroundColor = colorResource(pokemon.getTypeResColor()),
                elevation = 4.dp,
                shape = MaterialTheme.shapes.medium.copy(all = CornerSize(16.dp)),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        navController.navigate("detail/${pokemon.id}")
                    }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                        Text(
                            pokemon.name.capitalize(Locale.getDefault()),
                            maxLines = 1,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.white),
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentWidth(Alignment.Start)
                        )
                        Text(
                            String.format("#%03d", pokemon.id),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colorResource(R.color.white),
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentWidth(Alignment.End)
                        )
                    }
                    pokemon.imageUrl?.let {
                        CoilImage(
                            data = it,
                            contentDescription = pokemon.name,
                            fadeIn = true,
                            modifier = Modifier.height(128.dp)
                        )
                    }
                }
            }
        }
    }
}
